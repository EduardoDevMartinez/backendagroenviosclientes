package com.agroenvios.clientes.config;

import com.agroenvios.clientes.primary.model.ConfiguracionEnvio;
import com.agroenvios.clientes.primary.model.TarifaRangoEnvio;
import com.agroenvios.clientes.primary.repository.ConfiguracionEnvioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

/**
 * Siembra el tarifario inicial de envío la primera vez que arranca la app con las tablas vacías,
 * tomando los valores de las variables de entorno ENVIO_*. Crea un solo rango que cubre desde 0
 * hasta {@code envio.seed.radio-final-km}, para que el comportamiento sea idéntico al anterior.
 *
 * <p>A partir de ahí la única fuente de verdad son las tablas {@code configuracion_envio},
 * {@code tarifas_envio_rango} y {@code geocercas_envio}: cambiar las env vars ya no afecta
 * el cálculo. Los rangos reales se cargan por SQL (ver
 * {@code src/main/resources/db/envio-tarifas.sql}).
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class EnvioConfigInitializer {

    @Value("${envio.seed.origen.latitud:0}")
    private double origenLat;

    @Value("${envio.seed.origen.longitud:0}")
    private double origenLng;

    @Value("${envio.seed.tarifa-base:120.00}")
    private BigDecimal tarifaBase;

    @Value("${envio.seed.costo-por-km:20.00}")
    private BigDecimal costoPorKm;

    @Value("${envio.seed.costo-por-minuto:0.00}")
    private BigDecimal costoPorMinuto;

    @Value("${envio.seed.radio-final-km:9999.00}")
    private BigDecimal radioFinalKm;

    @Bean
    ApplicationRunner seedConfiguracionEnvio(ConfiguracionEnvioRepository repository) {
        return args -> {
            if (repository.findFirstByActivaTrueOrderByIdDesc().isPresent()) {
                return;
            }

            if (origenLat == 0 && origenLng == 0) {
                log.error("No hay tarifario de envío activo y ENVIO_ORIGEN_LAT/LNG no están definidas. " +
                        "Inserta una fila en configuracion_envio con sus rangos, o define las variables " +
                        "de entorno; mientras tanto /envio/cotizar responderá 503.");
                return;
            }

            ConfiguracionEnvio config = ConfiguracionEnvio.builder()
                    .nombre("Tarifario inicial")
                    .activa(true)
                    .origenLatitud(origenLat)
                    .origenLongitud(origenLng)
                    .build();

            TarifaRangoEnvio rango = TarifaRangoEnvio.builder()
                    .configuracion(config)
                    .nombre("Rango único")
                    .radioInicialKm(BigDecimal.ZERO)
                    .radioFinalKm(radioFinalKm)
                    .tarifaBase(tarifaBase)
                    .costoPorKm(costoPorKm)
                    .costoPorMinuto(costoPorMinuto)
                    .activa(true)
                    .build();

            config.setRangos(List.of(rango));
            config = repository.save(config);

            log.info("Tarifario de envío inicial creado (id={}): rango 0–{} km, base=${}, ${}/km, ${}/min, origen=({}, {})",
                    config.getId(), radioFinalKm, tarifaBase, costoPorKm, costoPorMinuto, origenLat, origenLng);
        };
    }
}
