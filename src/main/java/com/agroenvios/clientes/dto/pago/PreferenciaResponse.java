package com.agroenvios.clientes.dto.pago;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PreferenciaResponse {
    /** URL del Checkout Pro de MercadoPago para redirigir al usuario */
    private String initPoint;
}
