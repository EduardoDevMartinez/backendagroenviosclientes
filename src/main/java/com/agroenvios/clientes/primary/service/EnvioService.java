package com.agroenvios.clientes.primary.service;

import com.agroenvios.clientes.primary.dto.envio.CotizacionEnvio;
import com.agroenvios.clientes.primary.dto.envio.GeocercaAplicada;
import com.agroenvios.clientes.primary.model.ConfiguracionEnvio;
import com.agroenvios.clientes.primary.model.DireccionEntrega;
import com.agroenvios.clientes.primary.model.GeocercaEnvio;
import com.agroenvios.clientes.primary.model.TarifaRangoEnvio;
import com.agroenvios.clientes.primary.model.TipoGeocerca;
import com.agroenvios.clientes.primary.repository.ConfiguracionEnvioRepository;
import com.agroenvios.clientes.primary.repository.DireccionEntregaRepository;
import com.agroenvios.clientes.primary.repository.GeocercaEnvioRepository;
import com.agroenvios.clientes.primary.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Cotiza el envío usando exclusivamente datos de base de datos: el tarifario activo
 * ({@code configuracion_envio} + sus rangos en {@code tarifas_envio_rango}) y las zonas
 * de {@code geocercas_envio}.
 *
 * <p>Fórmula:
 * <pre>
 *   rango    = tramo donde cae distanciaKm (radioInicialKm ≤ d ≤ radioFinalKm)
 *
 *   subtotal = rango.tarifaBase
 *            + distanciaKm   * rango.costoPorKm
 *            + tiempoMinutos * rango.costoPorMinuto
 *
 *   tarifa   = subtotal * (multiplicadores de las geocercas del destino)
 *            + (recargos fijos de esas geocercas)
 *
 *   tarifa   = min(tarifa, tarifaMaxima)
 * </pre>
 *
 * Ejemplo: rango 0–10 km con base $120 y $20/km, entrega de 9 km → 120 + (9 × 20) = $300.
 *
 * <p>La distancia y el tiempo son de ruta real (OpenRouteService), no en línea recta.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EnvioService {

    private static final String ORS_URL = "https://api.openrouteservice.org/v2/directions/driving-car";

    @Value("${ors.api-key}")
    private String orsApiKey;

    private final DireccionEntregaRepository direccionRepository;
    private final ConfiguracionEnvioRepository configuracionRepository;
    private final GeocercaEnvioRepository geocercaRepository;
    private final RestTemplate restTemplate;

    // Sin @Transactional a propósito: la llamada HTTP a ORS tarda, y no queremos retener
    // una conexión del pool mientras tanto. Las geocercas llegan con sus puntos por JOIN FETCH.
    public CotizacionEnvio cotizar(Long direccionId) {
        DireccionEntrega dir = direccionRepository.findById(direccionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dirección no encontrada"));

        if (dir.getLatitud() == null || dir.getLongitud() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La dirección no tiene coordenadas. Actualízala con latitud y longitud.");
        }

        ConfiguracionEnvio config = configuracionRepository.findActivaConRangos()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "No hay un tarifario de envío activo configurado."));

        double[] ruta = llamarORS(config.getOrigenLatitud(), config.getOrigenLongitud(),
                dir.getLatitud(), dir.getLongitud());
        double distanciaKm = ruta[0];
        double tiempoMinutos = ruta[1];

        return calcular(config, dir.getLatitud(), dir.getLongitud(), distanciaKm, tiempoMinutos);
    }

    // ── Cálculo ───────────────────────────────────────────────────────────────

    private CotizacionEnvio calcular(ConfiguracionEnvio config, double latDestino, double lngDestino,
                                     double distanciaKm, double tiempoMinutos) {

        BigDecimal distancia = BigDecimal.valueOf(distanciaKm);
        BigDecimal tiempo = BigDecimal.valueOf(tiempoMinutos);

        TarifaRangoEnvio rango = rangoPara(config, distancia);

        BigDecimal costoDistancia = rango.getCostoPorKm().multiply(distancia)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal costoTiempo = rango.getCostoPorMinuto().multiply(tiempo)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal subtotal = rango.getTarifaBase()
                .add(costoDistancia)
                .add(costoTiempo)
                .setScale(2, RoundingMode.HALF_UP);

        List<GeocercaEnvio> aplicables = geocercasQueAplican(latDestino, lngDestino);

        BigDecimal multiplicador = BigDecimal.ONE;
        BigDecimal recargo = BigDecimal.ZERO;
        List<GeocercaAplicada> detalleGeocercas = new ArrayList<>();

        for (GeocercaEnvio g : aplicables) {
            multiplicador = multiplicador.multiply(g.getMultiplicador());
            recargo = recargo.add(g.getRecargoFijo());
            detalleGeocercas.add(new GeocercaAplicada(g.getId(), g.getNombre(),
                    g.getMultiplicador(), g.getRecargoFijo()));
        }

        BigDecimal tarifa = subtotal.multiply(multiplicador)
                .add(recargo)
                .setScale(2, RoundingMode.HALF_UP);

        if (config.getTarifaMaxima() != null) {
            tarifa = tarifa.min(config.getTarifaMaxima());
        }

        log.info("Cotización envío: distancia={}km, tiempo={}min, rango={} ({}-{}km), subtotal=${}, multiplicador={}, recargo=${}, tarifa=${}, geocercas={}",
                distanciaKm, tiempoMinutos, rango.getId(), rango.getRadioInicialKm(), rango.getRadioFinalKm(),
                subtotal, multiplicador, recargo, tarifa,
                detalleGeocercas.stream().map(GeocercaAplicada::getNombre).toList());

        return CotizacionEnvio.builder()
                .tarifa(tarifa)
                .distanciaKm(distanciaKm)
                .tiempoMinutos(tiempoMinutos)
                .rangoId(rango.getId())
                .rangoNombre(rango.getNombre())
                .radioInicialKm(rango.getRadioInicialKm())
                .radioFinalKm(rango.getRadioFinalKm())
                .tarifaBase(rango.getTarifaBase())
                .costoDistancia(costoDistancia)
                .costoTiempo(costoTiempo)
                .subtotal(subtotal)
                .multiplicadorGeocercas(multiplicador)
                .recargoGeocercas(recargo)
                .geocercas(detalleGeocercas)
                .configuracionId(config.getId())
                .build();
    }

    /**
     * Primer rango activo (de menor a mayor radio) donde cae la distancia, con ambos
     * extremos inclusive. Si dos rangos se tocan en la frontera (0–10 y 10–20), gana el
     * de menor radio. Si la distancia no cae en ninguno, no hay cobertura.
     */
    private TarifaRangoEnvio rangoPara(ConfiguracionEnvio config, BigDecimal distancia) {
        return config.getRangos().stream()
                .filter(r -> Boolean.TRUE.equals(r.getActiva()))
                .sorted(Comparator.comparing(TarifaRangoEnvio::getRadioInicialKm))
                .filter(r -> distancia.compareTo(r.getRadioInicialKm()) >= 0
                        && distancia.compareTo(r.getRadioFinalKm()) <= 0)
                .findFirst()
                .orElseThrow(() -> {
                    BigDecimal cobertura = config.getRangos().stream()
                            .filter(r -> Boolean.TRUE.equals(r.getActiva()))
                            .map(TarifaRangoEnvio::getRadioFinalKm)
                            .max(BigDecimal::compareTo)
                            .orElse(BigDecimal.ZERO);

                    log.info("Sin rango de tarifa para {} km (cobertura máxima {} km, tarifario id={})",
                            distancia, cobertura, config.getId());

                    return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "La dirección está a " + distancia.setScale(1, RoundingMode.HALF_UP)
                                    + " km y solo entregamos hasta "
                                    + cobertura.stripTrailingZeros().toPlainString() + " km.");
                });
    }

    /**
     * Geocercas activas que contienen al destino, ya resueltas por prioridad.
     * Si alguna bloquea el envío, corta con 400. Si alguna es exclusiva, se usa solo esa.
     */
    private List<GeocercaEnvio> geocercasQueAplican(double lat, double lng) {
        List<GeocercaEnvio> coincidencias = geocercaRepository.findActivasConPuntos().stream()
                .filter(g -> contiene(g, lat, lng))
                .toList();

        for (GeocercaEnvio g : coincidencias) {
            if (Boolean.TRUE.equals(g.getBloqueaEnvio())) {
                log.info("Destino ({}, {}) dentro de zona sin cobertura: {}", lat, lng, g.getNombre());
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Por ahora no entregamos en esta zona (" + g.getNombre() + ").");
            }
        }

        // findActivasConPuntos viene ordenado por prioridad DESC, así que la primera exclusiva gana.
        return coincidencias.stream()
                .filter(g -> Boolean.TRUE.equals(g.getExclusiva()))
                .findFirst()
                .map(List::of)
                .orElse(coincidencias);
    }

    private boolean contiene(GeocercaEnvio g, double lat, double lng) {
        if (g.getTipo() == TipoGeocerca.CIRCULO) {
            if (g.getCentroLatitud() == null || g.getCentroLongitud() == null || g.getRadioMetros() == null) {
                log.warn("Geocerca CIRCULO '{}' (id={}) sin centro o radio — se ignora", g.getNombre(), g.getId());
                return false;
            }
            return GeoUtils.distanciaMetros(lat, lng, g.getCentroLatitud(), g.getCentroLongitud())
                    <= g.getRadioMetros();
        }
        return GeoUtils.dentroDePoligono(lat, lng, g.getPuntos());
    }

    // ── OpenRouteService ──────────────────────────────────────────────────────

    /** Llama a OpenRouteService y devuelve [distanciaKm, tiempoMinutos] de la ruta en auto. */
    @SuppressWarnings("unchecked")
    private double[] llamarORS(double latOrigen, double lngOrigen, double latDestino, double lngDestino) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", orsApiKey);

        // ORS usa orden [longitud, latitud]
        Map<String, Object> body = Map.of(
                "coordinates", List.of(
                        List.of(lngOrigen, latOrigen),
                        List.of(lngDestino, latDestino)
                )
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(ORS_URL, entity, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenRouteService no devolvió respuesta");
            }

            List<Map<String, Object>> routes = (List<Map<String, Object>>) responseBody.get("routes");
            Map<String, Object> summary = (Map<String, Object>) routes.get(0).get("summary");

            double distanciaMetros = ((Number) summary.get("distance")).doubleValue();
            double duracionSegundos = ((Number) summary.get("duration")).doubleValue();

            double distanciaKm = Math.round(distanciaMetros / 10.0) / 100.0; // redondeo a 2 decimales
            double tiempoMinutos = Math.round(duracionSegundos / 6.0) / 10.0;

            return new double[]{distanciaKm, tiempoMinutos};
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al consultar OpenRouteService: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudo calcular la distancia. Intenta de nuevo.");
        }
    }
}
