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
public class ConfiguracionEnvioResponse {

    private Long id;
    private String nombre;
    private Double origenLatitud;
    private Double origenLongitud;
    private BigDecimal tarifaMaxima;
    private List<TarifaRangoResponse> rangos;
}
