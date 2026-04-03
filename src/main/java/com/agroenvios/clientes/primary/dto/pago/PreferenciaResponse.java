package com.agroenvios.clientes.primary.dto.pago;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PreferenciaResponse {
    private String initPoint;
    private String referenciaPago; // UUID para rastrear el pago desde la app
}
