package com.agroenvios.clientes.primary.controller;

import com.agroenvios.clientes.sse.SseEmitterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/products/webhook")
@RequiredArgsConstructor
public class ProductStockWebhookController {

    private final SseEmitterRegistry sseEmitterRegistry;

    @Value("${internal.api.key:}")
    private String internalApiKey;

    /**
     * Recibe de proveedores el stock nuevo de uno o varios productos (pedido creado,
     * confirmado o cancelado, o el comercio corrigió su inventario) y lo reparte por SSE a
     * todas las apps conectadas, para que el catálogo se actualice en vivo y nadie pida un
     * producto que ya se agotó. Llamada servidor-a-servidor, sin sesión de usuario: se
     * autentica con la misma API key compartida que el webhook de estado de pedidos.
     *
     * Cuerpo: [{"productId": 11, "stockAvailable": 7}, ...]
     */
    @PostMapping("/stock")
    public ResponseEntity<Void> stockActualizado(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @RequestBody List<Map<String, Object>> stock) {
        if (internalApiKey == null || internalApiKey.isBlank() || !internalApiKey.equals(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        sseEmitterRegistry.broadcast("stock-actualizado", stock);
        return ResponseEntity.ok().build();
    }
}
