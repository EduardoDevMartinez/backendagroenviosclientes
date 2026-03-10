package com.agroenvios.clientes.primary.service;

import com.agroenvios.clientes.primary.dto.pago.ItemPagoDto;
import com.agroenvios.clientes.primary.dto.pago.PreferenciaRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Integración con MercadoPago Checkout Pro.
 *
 * Flujo:
 *  1. El frontend envía los ítems del carrito y la dirección seleccionada.
 *  2. Este servicio llama a la API de MP y devuelve el init_point.
 *  3. El frontend abre esa URL (WebView o navegador externo).
 *  4. MP redirige al usuario de vuelta a la app tras el pago usando el esquema deep-link.
 *
 * Variable de entorno requerida: MP_ACCESS_TOKEN
 */
@Service
@Slf4j
public class MercadoPagoService {

    private static final String MP_PREFERENCES_URL = "https://api.mercadopago.com/checkout/preferences";

    @Value("${MP_ACCESS_TOKEN:}")
    private String accessToken;

    private final RestTemplate restTemplate = new RestTemplate();

    public String crearPreferencia(PreferenciaRequest request) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException("MP_ACCESS_TOKEN no está configurado");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        // Construir items para MP
        List<Map<String, Object>> mpItems = request.getItems().stream()
                .map(this::toMpItem)
                .toList();

        // Deep-links de retorno a la app
        Map<String, String> backUrls = new HashMap<>();
        backUrls.put("success", "agroenvios://pago/exito");
        backUrls.put("failure", "agroenvios://pago/fallo");
        backUrls.put("pending", "agroenvios://pago/pendiente");

        Map<String, Object> body = new HashMap<>();
        body.put("items", mpItems);
        body.put("back_urls", backUrls);
        body.put("auto_return", "approved");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        log.info("Creando preferencia de pago en MercadoPago para {} ítems", mpItems.size());

        ResponseEntity<Map> response = restTemplate.postForEntity(MP_PREFERENCES_URL, entity, Map.class);

        if (response.getBody() != null && response.getBody().containsKey("init_point")) {
            String initPoint = (String) response.getBody().get("init_point");
            log.info("Preferencia creada: {}", initPoint);
            return initPoint;
        }

        throw new RuntimeException("MercadoPago no devolvió un init_point válido");
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
