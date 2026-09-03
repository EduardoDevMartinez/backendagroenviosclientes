package com.agroenvios.clientes.primary.service;

import com.agroenvios.clientes.primary.dto.pago.ItemPagoDto;
import com.agroenvios.clientes.primary.model.DireccionEntrega;
import com.agroenvios.clientes.primary.model.Pedido;
import com.agroenvios.clientes.primary.repository.DireccionEntregaRepository;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Puente hacia el backend de proveedores: cuando un pedido se aprueba aquí, crea el
 * pedido correspondiente allá (POST /orders/external) para que el comercio lo vea y
 * pueda atenderlo. Es asíncrono y con try/catch silencioso a propósito: un fallo aquí
 * (proveedores caído, red, etc.) nunca debe afectar la confirmación del pedido local
 * ni la respuesta al webhook de MercadoPago.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalOrderBridgeService {

    private final RestTemplate restTemplate;
    private final DireccionEntregaRepository direccionEntregaRepository;

    @Value("${proveedores.api.base-url:}")
    private String proveedoresBaseUrl;

    @Value("${proveedores.internal.api.key:}")
    private String internalApiKey;

    @Async
    public void bridgeToProveedores(Pedido pedido, List<ItemPagoDto> items,
                                     String customerEmail, String customerName, String customerPhone) {
        if (proveedoresBaseUrl == null || proveedoresBaseUrl.isBlank()
                || internalApiKey == null || internalApiKey.isBlank()) {
            log.warn("Puente a proveedores no configurado (proveedores.api.base-url / proveedores.internal.api.key); " +
                    "pedido id={} no se replicó allá", pedido.getId());
            return;
        }

        List<ItemPagoDto> itemsConProducto = items.stream()
                .filter(i -> i.getProductId() != null && i.getTradeShopId() != null)
                .toList();

        if (itemsConProducto.isEmpty()) {
            log.warn("Pedido id={} no trae productId/tradeShopId en ningún item (carrito viejo o producto sin " +
                    "comercio asociado); no se puede replicar en proveedores", pedido.getId());
            return;
        }

        try {
            DireccionEntrega direccion = direccionEntregaRepository.findById(pedido.getDireccionId())
                    .orElseThrow(() -> new RuntimeException("Dirección no encontrada: " + pedido.getDireccionId()));

            List<Map<String, Object>> itemsPayload = itemsConProducto.stream()
                    .map(i -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("productId", i.getProductId());
                        item.put("selectedTradeShopId", i.getTradeShopId());
                        // El pedido de proveedores maneja cantidades enteras; los items por peso
                        // (kg/g/lb) se redondean al entero más cercano.
                        item.put("quantity", (int) Math.round(i.getCantidad()));
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
            body.put("deliveryAddress", direccion.getCalle());
            body.put("deliveryCity", direccion.getCiudad());
            body.put("deliveryState", direccion.getEstado());
            body.put("deliveryPostalCode", direccion.getCodigoPostal());
            body.put("deliveryColonia", direccion.getColonia());
            body.put("deliveryLatitude", direccion.getLatitud());
            body.put("deliveryLongitude", direccion.getLongitud());
            body.put("externalReference", pedido.getReferenciaPago());
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

            log.info("Pedido id={} replicado en proveedores (referencia={})", pedido.getId(), pedido.getReferenciaPago());
        } catch (Exception e) {
            log.error("Error replicando pedido id={} en proveedores: {}", pedido.getId(), e.getMessage(), e);
        }
    }
}
