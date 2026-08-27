package com.agroenvios.clientes.primary.dto.envio;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TarifaRangoRequest {

    private String nombre;

    private String color;

    @NotNull(message = "Debes indicar el radio inicial (km)")
    private BigDecimal radioInicialKm;

    @NotNull(message = "Debes indicar el radio final (km)")
    private BigDecimal radioFinalKm;

    @NotNull(message = "Debes indicar la tarifa base")
    private BigDecimal tarifaBase;

    @NotNull(message = "Debes indicar el costo por km")
    private BigDecimal costoPorKm;

    private BigDecimal costoPorMinuto;

    private Boolean activa;
}
