package com.agroenvios.clientes.primary.service;

import com.agroenvios.clientes.primary.dto.envio.CotizacionEnvio;
import com.agroenvios.clientes.primary.model.DireccionEntrega;
import com.agroenvios.clientes.primary.repository.DireccionEntregaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class EnvioService {

    private static final String ORS_URL = "https://api.openrouteservice.org/v2/directions/driving-car";

    @Value("${ors.api-key}")
    private String orsApiKey;

    @Value("${envio.origen.latitud}")
    private double origenLat;

    @Value("${envio.origen.longitud}")
    private double origenLng;

    @Value("${envio.tarifa-base:20.00}")
    private BigDecimal tarifaBase;

    @Value("${envio.precio-por-km:8.00}")
    private BigDecimal precioPorKm;

    private final DireccionEntregaRepository direccionRepository;
    private final RestTemplate restTemplate;

    public CotizacionEnvio cotizar(Long direccionId) {
        DireccionEntrega dir = direccionRepository.findById(direccionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dirección no encontrada"));

        if (dir.getLatitud() == null || dir.getLongitud() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La dirección no tiene coordenadas. Actualízala con latitud y longitud.");
        }

        double[] resultado = llamarORS(origenLat, origenLng, dir.getLatitud(), dir.getLongitud());
        double distanciaKm = resultado[0];
        double tiempoMinutos = resultado[1];

        BigDecimal tarifa = tarifaBase
                .add(precioPorKm.multiply(BigDecimal.valueOf(distanciaKm)))
                .setScale(2, RoundingMode.HALF_UP);

        log.info("Cotización envío: direccionId={}, distancia={}km, tiempo={}min, tarifa=${}",
                direccionId, distanciaKm, tiempoMinutos, tarifa);

        return new CotizacionEnvio(tarifa, distanciaKm, tiempoMinutos);
    }

    // Llama a OpenRouteService y devuelve [distanciaKm, tiempoMinutos]
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
