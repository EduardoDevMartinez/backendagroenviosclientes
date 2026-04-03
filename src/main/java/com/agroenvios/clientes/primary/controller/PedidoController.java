package com.agroenvios.clientes.primary.controller;

import com.agroenvios.clientes.primary.dto.pago.PedidoResponse;
import com.agroenvios.clientes.primary.service.PedidoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;

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
}
