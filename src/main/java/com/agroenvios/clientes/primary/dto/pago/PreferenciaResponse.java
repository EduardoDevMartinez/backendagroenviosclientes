package com.agroenvios.clientes.primary.dto.pago;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class PreferenciaResponse {
    private String initPoint;
    private String sandboxInitPoint;
    private String referenciaPago;
    private BigDecimal tarifaEnvio;
    private double distanciaKm;
    private double tiempoMinutos;
}
