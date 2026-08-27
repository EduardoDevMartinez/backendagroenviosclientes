package com.agroenvios.clientes.primary.controller;

import com.agroenvios.clientes.primary.dto.envio.ConfiguracionEnvioResponse;
import com.agroenvios.clientes.primary.dto.envio.CotizacionEnvio;
import com.agroenvios.clientes.primary.dto.envio.TarifaRangoRequest;
import com.agroenvios.clientes.primary.dto.envio.TarifaRangoResponse;
import com.agroenvios.clientes.primary.dto.envio.UpdateConfiguracionEnvioRequest;
import com.agroenvios.clientes.primary.service.EnvioConfigAdminService;
import com.agroenvios.clientes.primary.service.EnvioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * CRUD del tarifario de envío para el panel admin de proveedores. Llamada
 * servidor-a-servidor, sin sesión de usuario — se autentica con la misma API key
 * compartida que el resto del puente proveedores <-> clientes (ver
 * PedidoController.actualizarEstadoEntrega). No hay usuarios "administrador" en este
 * backend, así que este es el único mecanismo de protección para estos endpoints.
 */
@RestController
@RequestMapping("/envio/internal")
@RequiredArgsConstructor
public class EnvioConfigInternalController {

    private final EnvioConfigAdminService envioConfigAdminService;
    private final EnvioService envioService;

    @Value("${internal.api.key:}")
    private String internalApiKey;

    /**
     * Simulador de envío para el panel admin: misma cotización real que usa la app de
     * clientes (ruta por OpenRouteService, rango de tarifa, geocercas del punto), pero
     * para un punto cualquiera en vez de una dirección guardada. El 400 de "sin
     * cobertura"/"zona bloqueada" es un resultado esperado del simulador, no un error —
     * se devuelve tal cual con el mensaje real para que el admin lo vea.
     */
    @GetMapping("/simular")
    public ResponseEntity<?> simular(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @RequestParam double lat,
            @RequestParam double lng) {
        if (!isAuthorized(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            CotizacionEnvio cotizacion = envioService.cotizarPunto(lat, lng);
            return ResponseEntity.ok(cotizacion);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    @GetMapping("/config")
    public ResponseEntity<ConfiguracionEnvioResponse> getConfig(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
        if (!isAuthorized(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(envioConfigAdminService.getConfig());
    }

    @PutMapping("/config")
    public ResponseEntity<ConfiguracionEnvioResponse> updateConfig(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @Valid @RequestBody UpdateConfiguracionEnvioRequest request) {
        if (!isAuthorized(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(envioConfigAdminService.updateConfig(request));
    }

    @PostMapping("/config/rangos")
    public ResponseEntity<TarifaRangoResponse> createRango(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @Valid @RequestBody TarifaRangoRequest request) {
        if (!isAuthorized(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(envioConfigAdminService.createRango(request));
    }

    @PutMapping("/config/rangos/{id}")
    public ResponseEntity<TarifaRangoResponse> updateRango(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @PathVariable Long id,
            @Valid @RequestBody TarifaRangoRequest request) {
        if (!isAuthorized(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(envioConfigAdminService.updateRango(id, request));
    }

    @DeleteMapping("/config/rangos/{id}")
    public ResponseEntity<Void> deleteRango(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @PathVariable Long id) {
        if (!isAuthorized(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        envioConfigAdminService.deleteRango(id);
        return ResponseEntity.ok().build();
    }

    private boolean isAuthorized(String apiKey) {
        return internalApiKey != null && !internalApiKey.isBlank() && internalApiKey.equals(apiKey);
    }
}
