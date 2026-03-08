package com.agroenvios.clientes.controller;

import com.agroenvios.clientes.dto.pago.PreferenciaRequest;
import com.agroenvios.clientes.dto.pago.PreferenciaResponse;
import com.agroenvios.clientes.service.MercadoPagoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pagos")
@RequiredArgsConstructor
public class MercadoPagoController {

    private final MercadoPagoService mercadoPagoService;

    /**
     * Crea una preferencia de pago en MercadoPago Checkout Pro.
     * Requiere autenticación JWT.
     *
     * POST /pagos/preferencia
     * Body: { items: [{nombre, cantidad, precio}], direccionId }
     * Response: { initPoint: "https://www.mercadopago.com.mx/checkout/..." }
     */
    @PostMapping("/preferencia")
    public ResponseEntity<PreferenciaResponse> crearPreferencia(@RequestBody PreferenciaRequest request) {
        String initPoint = mercadoPagoService.crearPreferencia(request);
        return ResponseEntity.ok(new PreferenciaResponse(initPoint));
    }
}
