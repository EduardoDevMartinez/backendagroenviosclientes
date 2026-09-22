package com.agroenvios.clientes.primary.service;

import com.agroenvios.clientes.primary.dto.pago.ItemPagoDto;
import com.agroenvios.clientes.primary.model.Pedido;
import com.agroenvios.clientes.primary.model.PedidoItem;
import com.agroenvios.clientes.primary.model.User;
import com.agroenvios.clientes.primary.repository.PedidoRepository;
import com.agroenvios.clientes.primary.service.ExternalOrderBridgeService.Resultado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReplicacionPedidosServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 9, 21, 12, 0);

    private PedidoRepository pedidos;
    private ExternalOrderBridgeService puente;
    private ReplicacionPedidosService servicio;

    @BeforeEach
    void preparar() {
        pedidos = mock(PedidoRepository.class);
        puente = mock(ExternalOrderBridgeService.class);
        servicio = new ReplicacionPedidosService(pedidos, puente);
        when(puente.estaConfigurado()).thenReturn(true);
        when(puente.replicar(any(), anyList(), any(), any(), any(), any())).thenReturn(Resultado.REPLICADO);
    }

    private static Pedido pedido(long id, int intentos, LocalDateTime ultimoIntento) {
        User user = new User();
        user.setNombre("José Luis ");
        user.setPaterno("Guzmán");
        user.setCorreo("joseluisgc74@hotmail.com");
        user.setTelefono("4492279498");

        PedidoItem item = new PedidoItem();
        item.setNombre("Tilapia");
        item.setCantidad(1.5);
        item.setPrecioUnitario(new BigDecimal("61.75"));
        item.setProductId(89L);
        item.setTradeShopId(9L);

        Pedido p = new Pedido();
        p.setId(id);
        p.setUser(user);
        p.setItems(new ArrayList<>(List.of(item)));
        p.setReferenciaPago("ref-" + id);
        p.setCodigoEntrega("8222");
        p.setIntentosReplicacion(intentos);
        p.setUltimoIntentoReplicacionAt(ultimoIntento);
        return p;
    }

    private void hayPendientes(Pedido... lista) {
        when(pedidos.findPendientesDeReplicar(any(), any(), anyInt())).thenReturn(List.of(lista));
    }

    @Test
    @DisplayName("sin pedidos pendientes no llama a proveedores")
    void sinPendientes() {
        hayPendientes();

        servicio.reintentarPendientes(AHORA);

        verify(puente, never()).replicar(any(), anyList(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("con el puente sin configurar ni siquiera consulta la base")
    void puenteSinConfigurar() {
        when(puente.estaConfigurado()).thenReturn(false);

        servicio.reintentarPendientes(AHORA);

        verifyNoInteractions(pedidos);
        verify(puente, never()).replicar(any(), anyList(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("busca solo pedidos de las últimas 72 h y con al menos 3 min de antigüedad")
    void ventanaDeBusqueda() {
        hayPendientes();

        servicio.reintentarPendientes(AHORA);

        verify(pedidos).findPendientesDeReplicar(
                AHORA.minusHours(72), AHORA.minusMinutes(3), ExternalOrderBridgeService.MAX_INTENTOS);
    }

    @Test
    @DisplayName("reenvía con los mismos datos que el envío original (usuario, items, código de entrega)")
    void reenviaConLosDatosDelPedido() {
        Pedido p = pedido(7, 0, null);
        hayPendientes(p);

        servicio.reintentarPendientes(AHORA);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ItemPagoDto>> items = ArgumentCaptor.forClass(List.class);
        verify(puente).replicar(eq(p), items.capture(),
                eq("joseluisgc74@hotmail.com"), eq("José Luis  Guzmán"), eq("4492279498"), eq("8222"));

        assertEquals(1, items.getValue().size());
        ItemPagoDto enviado = items.getValue().get(0);
        assertEquals("Tilapia", enviado.getNombre());
        assertEquals(1.5, enviado.getCantidad());
        assertEquals(61.75, enviado.getPrecio());
        assertEquals(89L, enviado.getProductId());
        assertEquals(9L, enviado.getTradeShopId());
    }

    @Test
    @DisplayName("un pedido cuyo último intento fue hace poco espera; el que ya cumplió su espera se reintenta")
    void respetaLaEsperaEntreIntentos() {
        Pedido reciente = pedido(1, 1, AHORA.minusMinutes(1));   // con 1 fallo espera 5 min
        Pedido listo = pedido(2, 1, AHORA.minusMinutes(6));
        Pedido nuncaIntentado = pedido(3, 0, null);
        hayPendientes(reciente, listo, nuncaIntentado);

        servicio.reintentarPendientes(AHORA);

        verify(puente, never()).replicar(eq(reciente), anyList(), any(), any(), any(), any());
        verify(puente).replicar(eq(listo), anyList(), any(), any(), any(), any());
        verify(puente).replicar(eq(nuncaIntentado), anyList(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("la espera se duplica en cada fallo y se detiene en 6 horas")
    void esperaExponencialConTope() {
        assertEquals(Duration.ofMinutes(5), ReplicacionPedidosService.espera(0));
        assertEquals(Duration.ofMinutes(5), ReplicacionPedidosService.espera(1));
        assertEquals(Duration.ofMinutes(10), ReplicacionPedidosService.espera(2));
        assertEquals(Duration.ofMinutes(20), ReplicacionPedidosService.espera(3));
        assertEquals(Duration.ofMinutes(160), ReplicacionPedidosService.espera(6));
        assertEquals(Duration.ofHours(6), ReplicacionPedidosService.espera(8));
        assertEquals(Duration.ofHours(6), ReplicacionPedidosService.espera(ExternalOrderBridgeService.MAX_INTENTOS));
        assertEquals(Duration.ofHours(6), ReplicacionPedidosService.espera(Integer.MAX_VALUE));
    }

    @Test
    @DisplayName("un pedido que falla con excepción no impide reintentar los demás")
    void unFalloNoDetieneAlResto() {
        Pedido malo = pedido(1, 0, null);
        Pedido bueno = pedido(2, 0, null);
        hayPendientes(malo, bueno);
        when(puente.replicar(eq(malo), anyList(), any(), any(), any(), any())).thenThrow(new RuntimeException("boom"));

        servicio.reintentarPendientes(AHORA);

        verify(puente).replicar(eq(bueno), anyList(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("si la consulta a la base falla, la pasada termina sin lanzar (no tumba al planificador)")
    void fallaLaConsulta() {
        when(pedidos.findPendientesDeReplicar(any(), any(), anyInt())).thenThrow(new RuntimeException("bd caída"));

        servicio.reintentarPendientes(AHORA); // no debe lanzar

        verify(puente, never()).replicar(any(), anyList(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("procesa como máximo 25 pedidos por pasada")
    void topeDeLote() {
        Pedido[] muchos = new Pedido[40];
        for (int i = 0; i < muchos.length; i++) muchos[i] = pedido(i + 1, 0, null);
        hayPendientes(muchos);

        servicio.reintentarPendientes(AHORA);

        verify(puente, times(25)).replicar(any(), anyList(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("un pedido sin items se manda igual al puente, que es quien lo descarta")
    void pedidoSinItems() {
        Pedido p = pedido(1, 0, null);
        p.setItems(null);
        hayPendientes(p);

        servicio.reintentarPendientes(AHORA);

        verify(puente).replicar(eq(p), eq(List.of()), any(), any(), any(), any());
    }
}
