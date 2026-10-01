package com.agroenvios.clientes.primary.controller;

import com.agroenvios.clientes.primary.service.ClientesAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Directorio de clientes para el panel admin de proveedores. Llamada servidor-a-servidor
 * protegida con la API key compartida del puente (mismo esquema que
 * PagosClientesInternalController); este backend no tiene rol de administrador. Solo lectura.
 */
@RestController
@RequestMapping("/pedidos/internal/clientes")
@RequiredArgsConstructor
public class ClientesInternalController {

    private final ClientesAdminService clientesAdminService;

    @Value("${internal.api.key:}")
    private String internalApiKey;

    @GetMapping
    public ResponseEntity<?> listar(@RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
        if (!autorizado(apiKey)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(clientesAdminService.listar());
    }

    @GetMapping("/{clienteId}")
    public ResponseEntity<?> detalle(@PathVariable Long clienteId,
                                     @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
        if (!autorizado(apiKey)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return clientesAdminService.detalle(clienteId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    private boolean autorizado(String apiKey) {
        return internalApiKey != null && !internalApiKey.isBlank() && internalApiKey.equals(apiKey);
    }
}
