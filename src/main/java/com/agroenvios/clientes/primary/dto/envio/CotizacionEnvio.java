package com.agroenvios.clientes.primary.dto.envio;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class CotizacionEnvio {
    private BigDecimal tarifa;
    private double distanciaKm;
    private double tiempoMinutos;
}
