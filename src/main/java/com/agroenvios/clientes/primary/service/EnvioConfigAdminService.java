package com.agroenvios.clientes.primary.service;

import com.agroenvios.clientes.primary.dto.envio.ConfiguracionEnvioResponse;
import com.agroenvios.clientes.primary.dto.envio.TarifaRangoRequest;
import com.agroenvios.clientes.primary.dto.envio.TarifaRangoResponse;
import com.agroenvios.clientes.primary.dto.envio.UpdateConfiguracionEnvioRequest;
import com.agroenvios.clientes.primary.model.ConfiguracionEnvio;
import com.agroenvios.clientes.primary.model.TarifaRangoEnvio;
import com.agroenvios.clientes.primary.repository.ConfiguracionEnvioRepository;
import com.agroenvios.clientes.primary.repository.TarifaRangoEnvioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * CRUD del tarifario de envío (configuracion_envio + tarifas_envio_rango) para el panel
 * admin de proveedores, vía EnvioConfigInternalController. Separado de EnvioService a
 * propósito: ese es el motor de cotización (lectura, usado por la app de clientes), este
 * es edición pura del tarifario.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class EnvioConfigAdminService {

    private final ConfiguracionEnvioRepository configuracionRepository;
    private final TarifaRangoEnvioRepository rangoRepository;

    @Transactional(readOnly = true)
    public ConfiguracionEnvioResponse getConfig() {
        return toResponse(findActiva());
    }

    public ConfiguracionEnvioResponse updateConfig(UpdateConfiguracionEnvioRequest request) {
        ConfiguracionEnvio config = findActiva();
        config.setNombre(request.getNombre());
        config.setOrigenLatitud(request.getOrigenLatitud());
        config.setOrigenLongitud(request.getOrigenLongitud());
        config.setTarifaMaxima(request.getTarifaMaxima());
        configuracionRepository.save(config);
        return toResponse(config);
    }

    public TarifaRangoResponse createRango(TarifaRangoRequest request) {
        ConfiguracionEnvio config = findActiva();
        TarifaRangoEnvio rango = TarifaRangoEnvio.builder()
                .configuracion(config)
                .nombre(request.getNombre())
                .color(request.getColor() != null && !request.getColor().isBlank() ? request.getColor() : "#3B82F6")
                .radioInicialKm(request.getRadioInicialKm())
                .radioFinalKm(request.getRadioFinalKm())
                .tarifaBase(request.getTarifaBase())
                .costoPorKm(request.getCostoPorKm())
                .costoPorMinuto(request.getCostoPorMinuto() != null ? request.getCostoPorMinuto() : java.math.BigDecimal.ZERO)
                .activa(request.getActiva() != null ? request.getActiva() : true)
                .build();
        rango = rangoRepository.save(rango);
        return toResponse(rango);
    }

    public TarifaRangoResponse updateRango(Long rangoId, TarifaRangoRequest request) {
        TarifaRangoEnvio rango = rangoRepository.findById(rangoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rango no encontrado"));

        rango.setNombre(request.getNombre());
        if (request.getColor() != null && !request.getColor().isBlank()) {
            rango.setColor(request.getColor());
        }
        rango.setRadioInicialKm(request.getRadioInicialKm());
        rango.setRadioFinalKm(request.getRadioFinalKm());
        rango.setTarifaBase(request.getTarifaBase());
        rango.setCostoPorKm(request.getCostoPorKm());
        if (request.getCostoPorMinuto() != null) {
            rango.setCostoPorMinuto(request.getCostoPorMinuto());
        }
        if (request.getActiva() != null) {
            rango.setActiva(request.getActiva());
        }
        rangoRepository.save(rango);
        return toResponse(rango);
    }

    public void deleteRango(Long rangoId) {
        if (!rangoRepository.existsById(rangoId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rango no encontrado");
        }
        rangoRepository.deleteById(rangoId);
    }

    private ConfiguracionEnvio findActiva() {
        return configuracionRepository.findActivaConRangos()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "No hay un tarifario de envío activo configurado."));
    }

    private ConfiguracionEnvioResponse toResponse(ConfiguracionEnvio config) {
        List<TarifaRangoResponse> rangos = config.getRangos().stream()
                .map(this::toResponse)
                .toList();

        return ConfiguracionEnvioResponse.builder()
                .id(config.getId())
                .nombre(config.getNombre())
                .origenLatitud(config.getOrigenLatitud())
                .origenLongitud(config.getOrigenLongitud())
                .tarifaMaxima(config.getTarifaMaxima())
                .rangos(rangos)
                .build();
    }

    private TarifaRangoResponse toResponse(TarifaRangoEnvio rango) {
        return TarifaRangoResponse.builder()
                .id(rango.getId())
                .nombre(rango.getNombre())
                .color(rango.getColor())
                .radioInicialKm(rango.getRadioInicialKm())
                .radioFinalKm(rango.getRadioFinalKm())
                .tarifaBase(rango.getTarifaBase())
                .costoPorKm(rango.getCostoPorKm())
                .costoPorMinuto(rango.getCostoPorMinuto())
                .activa(rango.getActiva())
                .build();
    }
}
