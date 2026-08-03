package com.agroenvios.clientes.primary.dto.envio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CotizacionEnvio {

    /** Tarifa final a cobrar. */
    private BigDecimal tarifa;

    private double distanciaKm;
    private double tiempoMinutos;

    // ── Rango de distancia que aplicó ─────────────────────────────────────────
    private Long rangoId;
    private String rangoNombre;
    private BigDecimal radioInicialKm;
    private BigDecimal radioFinalKm;

    // ── Desglose del cálculo ──────────────────────────────────────────────────
    /** Tarifa base mínima del rango. */
    private BigDecimal tarifaBase;
    private BigDecimal costoDistancia;
    private BigDecimal costoTiempo;
    /** base + distancia + tiempo, antes de geocercas. */
    private BigDecimal subtotal;
    /** Producto de los multiplicadores de las geocercas aplicadas. */
    private BigDecimal multiplicadorGeocercas;
    /** Suma de los recargos fijos de las geocercas aplicadas. */
    private BigDecimal recargoGeocercas;
    private List<GeocercaAplicada> geocercas;
    /** Id del tarifario usado, para trazabilidad. */
    private Long configuracionId;
}
