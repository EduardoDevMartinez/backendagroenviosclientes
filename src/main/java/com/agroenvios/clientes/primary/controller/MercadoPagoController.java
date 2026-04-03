package com.agroenvios.clientes.primary.controller;

import com.agroenvios.clientes.primary.dto.pago.PreferenciaRequest;
import com.agroenvios.clientes.primary.dto.pago.PreferenciaResponse;
import com.agroenvios.clientes.primary.service.MercadoPagoService;
import com.agroenvios.clientes.primary.service.PedidoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/pagos")
@RequiredArgsConstructor
public class MercadoPagoController {

    private final MercadoPagoService mercadoPagoService;
    private final PedidoService pedidoService;

    @PostMapping("/preferencia")
    public ResponseEntity<PreferenciaResponse> crearPreferencia(@RequestBody PreferenciaRequest request) {
        return ResponseEntity.ok(mercadoPagoService.crearPreferencia(request));
    }

    /**
     * Webhook de MercadoPago — notifica cambios de estado de pagos.
     * Se responde 200 inmediatamente (MP espera máx 22 seg).
     * No requiere JWT (MP llama desde sus servidores).
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestHeader(value = "x-signature", required = false) String xSignature,
            @RequestHeader(value = "x-request-id", required = false) String xRequestId,
            @RequestParam(value = "data.id", required = false) String dataId,
            @RequestBody Map<String, Object> payload
    ) {
        ResponseEntity<Void> ok = ResponseEntity.ok().build();

        log.info("[WEBHOOK-DEBUG] x-signature={} | x-request-id={} | data.id={}",
                xSignature, xRequestId, dataId);

        if (!mercadoPagoService.validarFirmaWebhook(xSignature, xRequestId, dataId)) {
            log.warn("Webhook de MercadoPago rechazado: firma inválida");
            return ok;
        }

        String type = (String) payload.get("type");
        Map<String, Object> data = (Map<String, Object>) payload.get("data");

        if ("payment".equals(type) && data != null) {
            String pagoId = String.valueOf(data.get("id"));
            try {
                Map<String, Object> pago = mercadoPagoService.consultarPago(pagoId);
                String status = (String) pago.get("status");
                String externalReference = (String) pago.get("external_reference");
                log.info("Pago {} → status: {}, referencia: {}", pagoId, status, externalReference);
                pedidoService.procesarPago(externalReference, pagoId, status);
            } catch (Exception e) {
                log.error("Error al procesar webhook para pagoId={}: {}", pagoId, e.getMessage());
            }
        }

        return ok;
    }
}
