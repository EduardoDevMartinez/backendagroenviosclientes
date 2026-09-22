package com.agroenvios.clientes.primary.service;

import com.agroenvios.clientes.primary.dto.pago.ItemPagoDto;
import com.agroenvios.clientes.primary.model.DireccionEntrega;
import com.agroenvios.clientes.primary.model.Pedido;
import com.agroenvios.clientes.primary.repository.DireccionEntregaRepository;
import com.agroenvios.clientes.primary.repository.PedidoRepository;
import com.agroenvios.clientes.primary.service.ExternalOrderBridgeService.Resultado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.http.HttpStatus.CREATED;

/**
 * Prueba lo que deja el puente en el pedido según cómo le fue el envío a proveedores, y de
 * paso el cuerpo que manda (el mismo que usa el reintento automático).
 */
class ExternalOrderBridgeServiceReplicacionTest {

    private static final String URL = "https://proveedores.test/orders/external";
    private static final String LLAVE = "llave-de-prueba";
    private static final String REFERENCIA = "7ea91be9-1fa1-45b5-b65b-5a7f9e60278b";

    private MockRestServiceServer servidor;
    private DireccionEntregaRepository direcciones;
    private PedidoRepository pedidos;
    private ExternalOrderBridgeService puente;

    @BeforeEach
    void preparar() {
        RestTemplate restTemplate = new RestTemplate();
        servidor = MockRestServiceServer.createServer(restTemplate);
        direcciones = mock(DireccionEntregaRepository.class);
        pedidos = mock(PedidoRepository.class);
        puente = new ExternalOrderBridgeService(restTemplate, direcciones, pedidos);
        ReflectionTestUtils.setField(puente, "proveedoresBaseUrl", "https://proveedores.test");
        ReflectionTestUtils.setField(puente, "internalApiKey", LLAVE);

        when(direcciones.findById(12L)).thenReturn(Optional.of(DireccionEntrega.builder()
                .calle("Prolongación cosio sur 1042 int 12")
                .ciudad("Aguascalientes").estado("Aguascalientes")
                .codigoPostal(20240).colonia("La Salud")
                .latitud(21.8705949).longitud(-102.2859629)
                .build()));
    }

    private static Pedido pedido() {
        Pedido p = new Pedido();
        p.setId(7L);
        p.setDireccionId(12L);
        p.setReferenciaPago(REFERENCIA);
        p.setSubtotal(new BigDecimal("2000.38"));
        p.setTarifaEnvio(new BigDecimal("202.75"));
        p.setComisionMercadoPago(new BigDecimal("93.87"));
        return p;
    }

    private static ItemPagoDto item(Long productId, Long tradeShopId) {
        ItemPagoDto i = new ItemPagoDto();
        i.setNombre("Tilapia");
        i.setCantidad(1.5);
        i.setPrecio(61.75);
        i.setProductId(productId);
        i.setTradeShopId(tradeShopId);
        return i;
    }

    private Resultado replicar(Pedido pedido, List<ItemPagoDto> items) {
        return puente.replicar(pedido, items, "cliente@example.com", "José Luis Guzmán", "4492279498", "8222");
    }

    @Test
    @DisplayName("si proveedores responde 2xx: manda el pedido completo con la llave y lo marca como replicado")
    void exitoMarcaElPedidoComoReplicado() {
        servidor.expect(requestTo(URL))
                .andExpect(method(POST))
                .andExpect(header("X-Internal-Api-Key", LLAVE))
                .andExpect(jsonPath("$.externalReference").value(REFERENCIA))
                .andExpect(jsonPath("$.deliveryCode").value("8222"))
                .andExpect(jsonPath("$.customerEmail").value("cliente@example.com"))
                .andExpect(jsonPath("$.deliveryAddress").value("Prolongación cosio sur 1042 int 12"))
                .andExpect(jsonPath("$.items[0].productId").value(77))
                .andExpect(jsonPath("$.items[0].selectedTradeShopId").value(15))
                .andExpect(jsonPath("$.items[0].quantity").value(1.5))
                .andExpect(jsonPath("$.items[0].unitPrice").value(61.75))
                .andExpect(jsonPath("$.subtotal").value(2000.38))
                .andExpect(jsonPath("$.tarifaEnvio").value(202.75))
                .andRespond(withStatus(CREATED));

        assertEquals(Resultado.REPLICADO, replicar(pedido(), List.of(item(77L, 15L))));

        servidor.verify();
        verify(pedidos).marcarReplicado(eq(7L), any(LocalDateTime.class));
        verify(pedidos, never()).registrarFalloReplicacion(anyLong(), any(), anyString());
    }

    @Test
    @DisplayName("si proveedores rechaza con 400: devuelve ERROR y guarda su mensaje para diagnosticar")
    void rechazoDeProveedoresQuedaRegistrado() {
        servidor.expect(requestTo(URL)).andRespond(withBadRequest().body("Product not found: 77"));

        assertEquals(Resultado.ERROR, replicar(pedido(), List.of(item(77L, 15L))));

        verify(pedidos).registrarFalloReplicacion(eq(7L), any(LocalDateTime.class), contains("Product not found: 77"));
        verify(pedidos, never()).marcarReplicado(anyLong(), any());
    }

    @Test
    @DisplayName("si proveedores está caído (5xx): devuelve ERROR, para que se reintente")
    void proveedoresCaidoSePuedeReintentar() {
        servidor.expect(requestTo(URL)).andRespond(withServerError());

        assertEquals(Resultado.ERROR, replicar(pedido(), List.of(item(77L, 15L))));

        verify(pedidos).registrarFalloReplicacion(eq(7L), any(LocalDateTime.class), anyString());
    }

    @Test
    @DisplayName("si la dirección ya no existe: devuelve ERROR sin llamar a proveedores")
    void direccionInexistente() {
        when(direcciones.findById(12L)).thenReturn(Optional.empty());

        assertEquals(Resultado.ERROR, replicar(pedido(), List.of(item(77L, 15L))));

        servidor.verify(); // ninguna llamada HTTP esperada
        verify(pedidos).registrarFalloReplicacion(eq(7L), any(LocalDateTime.class), contains("Dirección no encontrada"));
    }

    @Test
    @DisplayName("items sin productId/tradeShopId no se pueden replicar nunca: se descartan sin llamar a proveedores")
    void sinReferenciaAProductoSeDescarta() {
        assertEquals(Resultado.DESCARTADO, replicar(pedido(), List.of(item(null, null))));

        servidor.verify();
        verify(pedidos).descartarReplicacion(eq(7L), any(LocalDateTime.class), anyString(),
                eq(ExternalOrderBridgeService.MAX_INTENTOS));
        verify(pedidos, never()).registrarFalloReplicacion(anyLong(), any(), anyString());
    }

    @Test
    @DisplayName("sin URL o llave configuradas no intenta nada ni toca el pedido")
    void sinConfiguracion() {
        ReflectionTestUtils.setField(puente, "internalApiKey", "");

        assertEquals(Resultado.NO_CONFIGURADO, replicar(pedido(), List.of(item(77L, 15L))));

        servidor.verify();
        verifyNoInteractions(pedidos);
    }

    @Test
    @DisplayName("si falla guardar la constancia, el resultado del envío no cambia")
    void fallaAlGuardarNoCambiaElResultado() {
        servidor.expect(requestTo(URL)).andRespond(withStatus(CREATED));
        when(pedidos.marcarReplicado(anyLong(), any())).thenThrow(new RuntimeException("bd caída"));

        assertEquals(Resultado.REPLICADO, replicar(pedido(), List.of(item(77L, 15L))));
        verify(pedidos, never()).registrarFalloReplicacion(anyLong(), any(), anyString());
    }

    @Test
    @DisplayName("un motivo de error larguísimo se recorta para caber en la columna")
    void recortaElMotivo() {
        servidor.expect(requestTo(URL)).andRespond(withBadRequest().body("x".repeat(2000)));

        replicar(pedido(), List.of(item(77L, 15L)));

        verify(pedidos).registrarFalloReplicacion(eq(7L), any(LocalDateTime.class),
                org.mockito.ArgumentMatchers.argThat(motivo -> motivo != null && motivo.length() <= 500));
        verify(pedidos, never()).descartarReplicacion(anyLong(), any(), anyString(), anyInt());
    }
}
