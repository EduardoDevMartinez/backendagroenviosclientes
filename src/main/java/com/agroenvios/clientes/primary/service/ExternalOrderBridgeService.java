package com.agroenvios.clientes.primary.service;

import com.agroenvios.clientes.primary.dto.pago.ItemPagoDto;
import com.agroenvios.clientes.primary.model.DireccionEntrega;
import com.agroenvios.clientes.primary.model.Pedido;
import com.agroenvios.clientes.primary.repository.DireccionEntregaRepository;
import com.agroenvios.clientes.primary.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Puente hacia el backend de proveedores: cuando un pedido se aprueba aquí, crea el
 * pedido correspondiente allá (POST /orders/external) para que el comercio lo vea y
 * pueda atenderlo. El envío inicial es asíncrono y con try/catch silencioso a propósito:
 * un fallo aquí (proveedores caído, red, etc.) nunca debe afectar la confirmación del
 * pedido local ni la respuesta al webhook de MercadoPago.
 *
 * Como el cliente ya pagó, un fallo no puede quedarse callado: cada envío deja constancia
 * en el propio pedido (replicado_proveedores_at / intentos / último error) y
 * {@link ReplicacionPedidosService} reintenta los que no llegaron. Reenviar es seguro:
 * proveedores identifica el pedido por su referencia de pago y ignora los duplicados.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalOrderBridgeService {

    /** Envíos fallidos tras los cuales se deja de reintentar solo (el inicial cuenta como el primero). */
    public static final int MAX_INTENTOS = 12;

    private static final int MAX_LARGO_ERROR = 500;

    public enum Resultado {
        /** Proveedores respondió 2xx: tiene el pedido (nuevo o ya existente). */
        REPLICADO,
        /** No llegó (red, proveedores caído, respuesta 4xx/5xx…): se puede reintentar. */
        ERROR,
        /** No se puede replicar nunca (sin productId/tradeShopId): no tiene caso reintentar. */
        DESCARTADO,
        /** Falta URL o llave de proveedores: no se intentó nada. */
        NO_CONFIGURADO
    }

    private final RestTemplate restTemplate;
    private final DireccionEntregaRepository direccionEntregaRepository;
    private final PedidoRepository pedidoRepository;

    @Value("${proveedores.api.base-url:}")
    private String proveedoresBaseUrl;

    @Value("${proveedores.internal.api.key:}")
    private String internalApiKey;

    public boolean estaConfigurado() {
        return proveedoresBaseUrl != null && !proveedoresBaseUrl.isBlank()
                && internalApiKey != null && !internalApiKey.isBlank();
    }

    /** Envío inicial, justo al aprobarse el pago: en otro hilo para no frenar la respuesta. */
    @Async
    public void bridgeToProveedores(Pedido pedido, List<ItemPagoDto> items,
                                     String customerEmail, String customerName, String customerPhone,
                                     String deliveryCode) {
        replicar(pedido, items, customerEmail, customerName, customerPhone, deliveryCode);
    }

    /**
     * Envía el pedido a proveedores en el hilo actual y deja constancia del resultado en el
     * pedido. Nunca lanza: cualquier fallo se devuelve como {@link Resultado#ERROR}.
     */
    public Resultado replicar(Pedido pedido, List<ItemPagoDto> items,
                              String customerEmail, String customerName, String customerPhone,
                              String deliveryCode) {
        if (!estaConfigurado()) {
            log.warn("Puente a proveedores no configurado (proveedores.api.base-url / proveedores.internal.api.key); " +
                    "pedido id={} no se replicó allá", pedido.getId());
            return Resultado.NO_CONFIGURADO;
        }

        List<ItemPagoDto> itemsConProducto = items.stream()
                .filter(i -> i.getProductId() != null && i.getTradeShopId() != null)
                .toList();

        if (itemsConProducto.isEmpty()) {
            log.warn("Pedido id={} no trae productId/tradeShopId en ningún item (carrito viejo o producto sin " +
                    "comercio asociado); no se puede replicar en proveedores", pedido.getId());
            registrar(() -> pedidoRepository.descartarReplicacion(pedido.getId(), LocalDateTime.now(),
                    "Sin productId/tradeShopId en ningún item: no se puede replicar", MAX_INTENTOS));
            return Resultado.DESCARTADO;
        }

        try {
            DireccionEntrega direccion = direccionEntregaRepository.findById(pedido.getDireccionId())
                    .orElseThrow(() -> new RuntimeException("Dirección no encontrada: " + pedido.getDireccionId()));

            List<Map<String, Object>> itemsPayload = itemsConProducto.stream()
                    .map(i -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("productId", i.getProductId());
                        item.put("selectedTradeShopId", i.getTradeShopId());
                        // El pedido de proveedores ya acepta cantidades fraccionarias (kg/g/lb),
                        // se manda la cantidad real sin redondear.
                        item.put("quantity", i.getCantidad());
                        // Precio realmente pagado por el cliente para este item — evita que
                        // proveedores tenga que recalcularlo desde su propio catálogo, que puede
                        // haber cambiado desde que se hizo el pedido.
                        item.put("unitPrice", i.getPrecio());
                        return item;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> body = new HashMap<>();
            body.put("customerEmail", customerEmail);
            body.put("customerName", customerName);
            body.put("customerPhone", customerPhone);
            body.put("deliveryAddress", calleCompleta(direccion));
            body.put("deliveryCity", direccion.getCiudad());
            body.put("deliveryState", direccion.getEstado());
            body.put("deliveryPostalCode", direccion.getCodigoPostal());
            body.put("deliveryColonia", direccion.getColonia());
            body.put("deliveryLatitude", direccion.getLatitud());
            body.put("deliveryLongitude", direccion.getLongitud());
            body.put("externalReference", pedido.getReferenciaPago());
            // Folio compartido: proveedores muestra este mismo número al comercio, al
            // fletista y al admin, así el "Pedido #N" es igual en todas las apps.
            body.put("clientOrderNumber", pedido.getId());
            body.put("deliveryCode", deliveryCode);
            body.put("items", itemsPayload);
            // Montos reales del pago ya aprobado — permiten que proveedores calcule la
            // comisión del comercio y pague al fletista sobre el envío real, no adivinado.
            body.put("subtotal", pedido.getSubtotal());
            body.put("tarifaEnvio", pedido.getTarifaEnvio());
            body.put("comisionMercadoPago", pedido.getComisionMercadoPago());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Api-Key", internalApiKey);

            restTemplate.exchange(
                    proveedoresBaseUrl + "/orders/external",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );
        } catch (Exception e) {
            String motivo = describir(e);
            log.error("Error replicando pedido id={} en proveedores: {}", pedido.getId(), motivo, e);
            registrar(() -> pedidoRepository.registrarFalloReplicacion(pedido.getId(), LocalDateTime.now(), motivo));
            return Resultado.ERROR;
        }

        log.info("Pedido id={} replicado en proveedores (referencia={})", pedido.getId(), pedido.getReferenciaPago());
        registrar(() -> pedidoRepository.marcarReplicado(pedido.getId(), LocalDateTime.now()));
        return Resultado.REPLICADO;
    }

    /**
     * Guardar la constancia nunca debe tumbar el envío ni cambiar su resultado: si la BD falla
     * aquí, lo peor es un reintento de más, que proveedores ignora por ser idempotente.
     */
    private void registrar(Runnable escritura) {
        try {
            escritura.run();
        } catch (Exception e) {
            log.warn("No se pudo guardar el estado de replicación a proveedores: {}", e.getMessage());
        }
    }

    // Incluye la respuesta de proveedores cuando la hay (su controller devuelve el mensaje
    // de la excepción en el cuerpo del 400), que es justo lo que hace falta para diagnosticar.
    private static String describir(Exception e) {
        String mensaje = e.getMessage() == null ? "" : e.getMessage();
        String texto = e.getClass().getSimpleName() + (mensaje.isBlank() ? "" : ": " + mensaje);
        return texto.length() <= MAX_LARGO_ERROR ? texto : texto.substring(0, MAX_LARGO_ERROR);
    }

    /**
     * Línea de calle tal como debe verla el fletista: "Av Juárez 816 Int. 3". Proveedores
     * solo maneja un campo de dirección, así que los números se le mandan ya integrados.
     * Las direcciones anteriores a los campos separados traen el número dentro de
     * {@code calle} y sus números vienen null, por lo que salen igual que antes.
     */
    static String calleCompleta(DireccionEntrega direccion) {
        StringBuilder sb = new StringBuilder(direccion.getCalle() == null ? "" : direccion.getCalle().trim());
        String exterior = direccion.getNumeroExterior();
        String interior = direccion.getNumeroInterior();
        if (exterior != null && !exterior.isBlank()) sb.append(' ').append(exterior.trim());
        if (interior != null && !interior.isBlank()) sb.append(" Int. ").append(interior.trim());
        return sb.toString();
    }
}
