package com.agroenvios.clientes.primary.service;

import com.agroenvios.clientes.primary.dto.envio.CotizacionEnvio;
import com.agroenvios.clientes.primary.model.*;
import com.agroenvios.clientes.primary.repository.ConfiguracionEnvioRepository;
import com.agroenvios.clientes.primary.repository.DireccionEntregaRepository;
import com.agroenvios.clientes.primary.repository.GeocercaEnvioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnvioServiceTest {

    private static final double DESTINO_LAT = 21.8818;
    private static final double DESTINO_LNG = -102.2916;

    @Mock
    private DireccionEntregaRepository direccionRepository;
    @Mock
    private ConfiguracionEnvioRepository configuracionRepository;
    @Mock
    private GeocercaEnvioRepository geocercaRepository;
    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private EnvioService envioService;

    private ConfiguracionEnvio config;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(envioService, "orsApiKey", "test-key");

        config = ConfiguracionEnvio.builder()
                .id(1L)
                .nombre("Test")
                .activa(true)
                .origenLatitud(21.9125)
                .origenLongitud(-102.2945)
                .rangos(new ArrayList<>())
                .build();

        // 0–10 km: base $120 + $20/km
        config.getRangos().add(rango(1L, "Local", "0", "10", "120", "20", "0"));
        // 10–25 km: base $180 + $18/km
        config.getRangos().add(rango(2L, "Metropolitana", "10", "25", "180", "18", "0"));
    }

    private TarifaRangoEnvio rango(Long id, String nombre, String desde, String hasta,
                                   String base, String porKm, String porMinuto) {
        return TarifaRangoEnvio.builder()
                .id(id)
                .configuracion(config)
                .nombre(nombre)
                .radioInicialKm(new BigDecimal(desde))
                .radioFinalKm(new BigDecimal(hasta))
                .tarifaBase(new BigDecimal(base))
                .costoPorKm(new BigDecimal(porKm))
                .costoPorMinuto(new BigDecimal(porMinuto))
                .activa(true)
                .build();
    }

    /** Prepara los mocks para una ruta de la distancia y duración dadas. */
    private void rutaDe(double km, double minutos) {
        DireccionEntrega direccion = DireccionEntrega.builder()
                .id(7L)
                .latitud(DESTINO_LAT)
                .longitud(DESTINO_LNG)
                .build();

        when(direccionRepository.findById(7L)).thenReturn(Optional.of(direccion));
        when(configuracionRepository.findActivaConRangos()).thenReturn(Optional.of(config));

        Map<String, Object> summary = Map.of("distance", km * 1000, "duration", minutos * 60);
        Map<String, Object> body = Map.of("routes", List.of(Map.of("summary", summary)));
        // lenient: al rechazar por zona de entrega ORS ni siquiera se consulta
        lenient().when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any()))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));
    }

    @Test
    @DisplayName("9 km en el rango 0–10: $120 de base + 9 × $20 = $300")
    void cotizaConLaTarifaDelRangoQueAplica() {
        rutaDe(9, 20);
        when(geocercaRepository.findActivasConPuntos()).thenReturn(List.of());

        CotizacionEnvio cotizacion = envioService.cotizar(7L);

        assertThat(cotizacion.getTarifa()).isEqualByComparingTo("300.00");
        assertThat(cotizacion.getRangoNombre()).isEqualTo("Local");
        assertThat(cotizacion.getTarifaBase()).isEqualByComparingTo("120");
        assertThat(cotizacion.getCostoDistancia()).isEqualByComparingTo("180.00");
        assertThat(cotizacion.getCostoTiempo()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("15 km cae en el segundo rango: $180 + 15 × $18 = $450")
    void cambiaDeRangoSegunLaDistancia() {
        rutaDe(15, 30);
        when(geocercaRepository.findActivasConPuntos()).thenReturn(List.of());

        CotizacionEnvio cotizacion = envioService.cotizar(7L);

        assertThat(cotizacion.getTarifa()).isEqualByComparingTo("450.00");
        assertThat(cotizacion.getRangoNombre()).isEqualTo("Metropolitana");
    }

    @Test
    @DisplayName("En la frontera exacta (10 km) gana el rango de menor radio")
    void laFronteraLaGanaElRangoMenor() {
        rutaDe(10, 22);
        when(geocercaRepository.findActivasConPuntos()).thenReturn(List.of());

        CotizacionEnvio cotizacion = envioService.cotizar(7L);

        // 120 + 10 × 20 = 320
        assertThat(cotizacion.getTarifa()).isEqualByComparingTo("320.00");
        assertThat(cotizacion.getRangoNombre()).isEqualTo("Local");
    }

    @Test
    @DisplayName("El costo por minuto se suma cuando el rango lo define")
    void cobraElTiempoCuandoEstaConfigurado() {
        config.getRangos().clear();
        config.getRangos().add(rango(3L, "Con tiempo", "0", "50", "120", "20", "2.50"));
        rutaDe(9, 20);
        when(geocercaRepository.findActivasConPuntos()).thenReturn(List.of());

        CotizacionEnvio cotizacion = envioService.cotizar(7L);

        // 120 + (9 × 20) + (20 × 2.50) = 350
        assertThat(cotizacion.getTarifa()).isEqualByComparingTo("350.00");
        assertThat(cotizacion.getCostoTiempo()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("La geocerca del destino multiplica el resultado del rango")
    void aplicaElMultiplicadorDeLaGeocerca() {
        rutaDe(9, 20);
        when(geocercaRepository.findActivasConPuntos()).thenReturn(List.of(
                GeocercaEnvio.builder()
                        .id(1L)
                        .nombre("Centro")
                        .tipo(TipoGeocerca.CIRCULO)
                        .multiplicador(new BigDecimal("1.300"))
                        .recargoFijo(new BigDecimal("50.00"))
                        .prioridad(10)
                        .activa(true)
                        .exclusiva(false)
                        .bloqueaEnvio(false)
                        .centroLatitud(DESTINO_LAT)
                        .centroLongitud(DESTINO_LNG)
                        .radioMetros(2500d)
                        .build()));

        CotizacionEnvio cotizacion = envioService.cotizar(7L);

        // (120 + 180) × 1.3 + 50 = 440
        assertThat(cotizacion.getTarifa()).isEqualByComparingTo("440.00");
        assertThat(cotizacion.getGeocercas()).hasSize(1);
    }

    private GeocercaEnvio circulo(Long id, String nombre, double lat, double lng, double radio, String mult) {
        return GeocercaEnvio.builder()
                .id(id)
                .nombre(nombre)
                .tipo(TipoGeocerca.CIRCULO)
                .multiplicador(new BigDecimal(mult))
                .recargoFijo(BigDecimal.ZERO)
                .prioridad(1)
                .activa(true)
                .exclusiva(false)
                .bloqueaEnvio(false)
                .centroLatitud(lat)
                .centroLongitud(lng)
                .radioMetros(radio)
                .build();
    }

    @Test
    @DisplayName("Una geocerca lejana no afecta la tarifa")
    void ignoraGeocercasQueNoContienenAlDestino() {
        rutaDe(9, 20);
        when(geocercaRepository.findActivasConPuntos()).thenReturn(List.of(
                circulo(1L, "Zona de entrega", DESTINO_LAT, DESTINO_LNG, 20000d, "1.000"),
                circulo(2L, "Otra ciudad", 19.4326, -99.1332, 5000d, "2.000")));

        CotizacionEnvio cotizacion = envioService.cotizar(7L);

        assertThat(cotizacion.getTarifa()).isEqualByComparingTo("300.00");
        assertThat(cotizacion.getGeocercas()).hasSize(1);
    }

    @Test
    @DisplayName("Fuera de la geocerca más grande no se puede pedir, aunque caiga en una chica")
    void rechazaDestinosFueraDeLaGeocercaMasGrande() {
        rutaDe(9, 20);
        when(geocercaRepository.findActivasConPuntos()).thenReturn(List.of(
                circulo(1L, "Zona de entrega", 19.4326, -99.1332, 20000d, "1.000"),
                circulo(2L, "Centro", DESTINO_LAT, DESTINO_LNG, 2500d, "1.300")));

        assertThatThrownBy(() -> envioService.cotizar(7L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("fuera de nuestra zona de entrega");
        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("Una zona que bloquea el envío no cuenta como zona de entrega")
    void laZonaBloqueadaNoDefineLaCobertura() {
        rutaDe(9, 20);
        GeocercaEnvio bloqueada = circulo(1L, "Bloqueada lejana", 19.4326, -99.1332, 50000d, "1.000");
        bloqueada.setBloqueaEnvio(true);
        when(geocercaRepository.findActivasConPuntos()).thenReturn(List.of(
                bloqueada,
                circulo(2L, "Zona de entrega", DESTINO_LAT, DESTINO_LNG, 5000d, "1.000")));

        assertThat(envioService.cotizar(7L).getTarifa()).isEqualByComparingTo("300.00");
    }

    @Test
    @DisplayName("Sin rango que cubra la distancia, no hay cobertura")
    void rechazaDistanciasFueraDeTodoRango() {
        rutaDe(40, 60);

        assertThatThrownBy(() -> envioService.cotizar(7L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("solo entregamos hasta 25 km");
    }

    @Test
    @DisplayName("Una geocerca con bloqueaEnvio corta la cotización")
    void rechazaZonasSinCobertura() {
        rutaDe(9, 20);
        when(geocercaRepository.findActivasConPuntos()).thenReturn(List.of(
                GeocercaEnvio.builder()
                        .id(3L)
                        .nombre("Zona no cubierta")
                        .tipo(TipoGeocerca.CIRCULO)
                        .multiplicador(BigDecimal.ONE)
                        .recargoFijo(BigDecimal.ZERO)
                        .prioridad(100)
                        .activa(true)
                        .exclusiva(false)
                        .bloqueaEnvio(true)
                        .centroLatitud(DESTINO_LAT)
                        .centroLongitud(DESTINO_LNG)
                        .radioMetros(3000d)
                        .build()));

        assertThatThrownBy(() -> envioService.cotizar(7L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Zona no cubierta");
    }

    @Test
    @DisplayName("El tope de tarifaMaxima recorta el resultado final")
    void respetaLaTarifaMaxima() {
        config.setTarifaMaxima(new BigDecimal("250.00"));
        rutaDe(9, 20);
        when(geocercaRepository.findActivasConPuntos()).thenReturn(List.of());

        CotizacionEnvio cotizacion = envioService.cotizar(7L);

        assertThat(cotizacion.getTarifa()).isEqualByComparingTo("250.00");
    }
}
