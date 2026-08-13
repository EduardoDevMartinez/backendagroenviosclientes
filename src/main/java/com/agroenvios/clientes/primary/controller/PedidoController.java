package com.agroenvios.clientes.primary.controller;

import com.agroenvios.clientes.primary.dto.pago.PedidoResponse;
import com.agroenvios.clientes.primary.service.PedidoService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;

    @Value("${internal.api.key:}")
    private String internalApiKey;

    @GetMapping
    public ResponseEntity<List<PedidoResponse>> getMisPedidos(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(pedidoService.getMisPedidos(userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> getPedido(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(pedidoService.getPedidoById(id, userDetails.getUsername()));
    }

    /**
     * Recibe el cambio de estado de un pedido desde proveedores (comercio confirmó,
     * listo, en camino, entregado...). Llamada servidor-a-servidor, sin sesión de
     * usuario — se autentica con la misma API key compartida que usa el puente en la
     * dirección contraria (POST /orders/external en proveedores).
     */
    @PostMapping("/webhook/estado-entrega")
    public ResponseEntity<Void> actualizarEstadoEntrega(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @RequestBody Map<String, String> body) {
        if (internalApiKey == null || internalApiKey.isBlank() || !internalApiKey.equals(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        pedidoService.actualizarEstadoEntrega(body.get("externalReference"), body.get("status"));
        return ResponseEntity.ok().build();
    }
}
