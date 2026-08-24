package com.agroenvios.clientes.primary.dto.envio;

import jakarta.validation.constraints.NotBlank;
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
public class UpdateConfiguracionEnvioRequest {

    @NotBlank(message = "Debes indicar un nombre")
    private String nombre;

    @NotNull(message = "Debes indicar la latitud de origen")
    private Double origenLatitud;

    @NotNull(message = "Debes indicar la longitud de origen")
    private Double origenLongitud;

    // Null = sin tope
    private BigDecimal tarifaMaxima;
}
