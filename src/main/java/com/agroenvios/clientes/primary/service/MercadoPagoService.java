package com.agroenvios.clientes.primary.service;

import com.agroenvios.clientes.primary.dto.pago.ItemPagoDto;
import com.agroenvios.clientes.primary.dto.pago.PreferenciaRequest;
import com.agroenvios.clientes.primary.dto.pago.PreferenciaResponse;
import com.agroenvios.clientes.primary.model.PagoPendiente;
import com.agroenvios.clientes.primary.model.User;
import com.agroenvios.clientes.primary.repository.PagoPendienteRepository;
import com.agroenvios.clientes.primary.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Integración con MercadoPago Checkout Pro.
 *
 * Flujo:
 *  1. El frontend envía los ítems del carrito y la dirección seleccionada.
 *  2. Este servicio guarda un PagoPendiente con UUID y llama a la API de MP.
 *  3. Devuelve el init_point y el UUID (referenciaPago) al frontend.
 *  4. El frontend abre el init_point en WebView o navegador externo.
 *  5. MP notifica el resultado del pago vía webhook → PedidoService crea el pedido.
 *
 * Variables de entorno requeridas: MP_ACCESS_TOKEN, MP_WEBHOOK_SECRET
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MercadoPagoService {

    private static final String MP_PREFERENCES_URL = "https://api.mercadopago.com/checkout/preferences";

    @Value("${mp.access.token:}")
    private String accessToken;

    @Value("${mp.webhook.secret:}")
    private String webhookSecret;

    /** Solo para pruebas — poner en false en producción */
    @Value("${mp.webhook.skip-validation:false}")
    private boolean skipWebhookValidation;

    private final RestTemplate restTemplate;
    private final PagoPendienteRepository pagoPendienteRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public PreferenciaResponse crearPreferencia(PreferenciaRequest request) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException("MP_ACCESS_TOKEN no está configurado");
        }

        // Obtener usuario autenticado
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + username));

        // Generar UUID como referencia de pago (external_reference para MP)
        String referenciaPago = UUID.randomUUID().toString();

        // Guardar sesión de pago temporal
        String itemsJson;
        try {
            itemsJson = objectMapper.writeValueAsString(request.getItems());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error al serializar items del carrito", e);
        }

        PagoPendiente pagoPendiente = new PagoPendiente();
        pagoPendiente.setId(referenciaPago);
        pagoPendiente.setUser(user);
        pagoPendiente.setItemsJson(itemsJson);
        pagoPendiente.setDireccionId(request.getDireccionId());
        pagoPendiente.setEstado("PENDIENTE");
        pagoPendiente.setExpiresAt(LocalDateTime.now().plusHours(24));
        pagoPendienteRepository.save(pagoPendiente);

        // Construir request para MP
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        List<Map<String, Object>> mpItems = request.getItems().stream()
                .map(this::toMpItem)
                .toList();

        Map<String, String> backUrls = new HashMap<>();
        backUrls.put("success", "agroenvios://pago/exito");
        backUrls.put("failure", "agroenvios://pago/fallo");
        backUrls.put("pending", "agroenvios://pago/pendiente");

        Map<String, Object> body = new HashMap<>();
        body.put("items", mpItems);
        body.put("back_urls", backUrls);
        body.put("external_reference", referenciaPago);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        log.info("Creando preferencia MP para {} ítems, usuario={}, referencia={}",
                mpItems.size(), username, referenciaPago);

        ResponseEntity<Map> response = restTemplate.postForEntity(MP_PREFERENCES_URL, entity, Map.class);

        if (response.getBody() != null) {
            String initPoint = (String) response.getBody().get("init_point");
            if (initPoint != null) {
                log.info("Preferencia creada: referencia={}", referenciaPago);
                return new PreferenciaResponse(initPoint, referenciaPago);
            }
        }

        throw new RuntimeException("MercadoPago no devolvió un init_point válido");
    }

    /**
     * Valida la firma HMAC-SHA256 del webhook de MercadoPago.
     * Retorna false si la firma es inválida o si MP_WEBHOOK_SECRET no está configurado.
     */
    public boolean validarFirmaWebhook(String xSignature, String xRequestId, String dataId) {
        if (skipWebhookValidation) {
            log.warn("[WEBHOOK] Validación de firma DESACTIVADA — solo para pruebas");
            return true;
        }

        log.info("[WEBHOOK] x-signature={} | x-request-id={} | data.id={}", xSignature, xRequestId, dataId);

        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.error("[WEBHOOK] MP_WEBHOOK_SECRET no configurado — rechazando");
            return false;
        }
        if (xSignature == null || xRequestId == null || dataId == null) {
            log.warn("[WEBHOOK] Parámetros nulos — xSignature={} xRequestId={} dataId={}", xSignature, xRequestId, dataId);
            return false;
        }

        String ts = null;
        String receivedHash = null;
        for (String part : xSignature.split(",")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length == 2) {
                if ("ts".equals(kv[0].trim())) ts = kv[1].trim();
                if ("v1".equals(kv[0].trim())) receivedHash = kv[1].trim();
            }
        }

        if (ts == null || receivedHash == null) {
            log.warn("[WEBHOOK] No se pudo parsear ts o v1 del x-signature");
            return false;
        }

        String manifest = "id:" + dataId + ";request-id:" + xRequestId + ";ts:" + ts + ";";
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            // El secret es hex — se decodifica a bytes antes de usarlo como clave HMAC
            byte[] secretBytes = HexFormat.of().parseHex(webhookSecret);
            mac.init(new SecretKeySpec(secretBytes, "HmacSHA256"));
            String expectedHash = HexFormat.of().formatHex(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)));
            log.info("[WEBHOOK] match={}", expectedHash.equals(receivedHash));
            return expectedHash.equals(receivedHash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("[WEBHOOK] Error HMAC: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Consulta el estado de un pago en MercadoPago por su ID.
     */
    public Map<String, Object> consultarPago(String pagoId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://api.mercadopago.com/v1/payments/" + pagoId,
                    HttpMethod.GET, entity, Map.class
            );
            return response.getBody();
        } catch (HttpStatusCodeException e) {
            log.error("Error al consultar pago {} en MercadoPago: status={}, body={}",
                    pagoId, e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }

    private Map<String, Object> toMpItem(ItemPagoDto item) {
        Map<String, Object> mpItem = new HashMap<>();
        mpItem.put("title", item.getNombre());
        mpItem.put("quantity", item.getCantidad());
        mpItem.put("unit_price", item.getPrecio());
        mpItem.put("currency_id", "MXN");
        return mpItem;
    }
}
