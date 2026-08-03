package com.agroenvios.clientes.primary.dto.envio;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/** Geocerca que afectó la tarifa de una cotización. */
@Data
@AllArgsConstructor
public class GeocercaAplicada {
    private Long id;
    private String nombre;
    private BigDecimal multiplicador;
    private BigDecimal recargoFijo;
}
