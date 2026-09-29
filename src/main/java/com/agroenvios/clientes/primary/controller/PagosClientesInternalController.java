package com.agroenvios.clientes.primary.controller;

import com.agroenvios.clientes.primary.dto.pago.PagoClienteAdminResponse;
import com.agroenvios.clientes.primary.service.PagosClientesAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Historial de pagos de clientes para el panel admin de proveedores. Llamada
 * servidor-a-servidor protegida con la API key compartida del puente (mismo esquema que
 * EnvioConfigInternalController); este backend no tiene rol de administrador.
 */
@RestController
@RequestMapping("/pedidos/internal")
@RequiredArgsConstructor
public class PagosClientesInternalController {

    private final PagosClientesAdminService pagosClientesAdminService;

    @Value("${internal.api.key:}")
    private String internalApiKey;

    @GetMapping("/pagos")
    public ResponseEntity<List<PagoClienteAdminResponse>> getPagos(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
        if (internalApiKey == null || internalApiKey.isBlank() || !internalApiKey.equals(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(pagosClientesAdminService.listarPagos());
    }
}
