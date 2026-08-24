package com.agroenvios.clientes.primary.dto.envio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TarifaRangoResponse {

    private Long id;
    private String nombre;
    private BigDecimal radioInicialKm;
    private BigDecimal radioFinalKm;
    private BigDecimal tarifaBase;
    private BigDecimal costoPorKm;
    private BigDecimal costoPorMinuto;
    private Boolean activa;
}
