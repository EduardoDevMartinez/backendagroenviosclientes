package com.agroenvios.clientes.primary.service;

import com.agroenvios.clientes.primary.dto.pago.PedidoPageDTO;
import com.agroenvios.clientes.primary.model.Pedido;
import com.agroenvios.clientes.primary.model.User;
import com.agroenvios.clientes.primary.repository.PedidoRepository;
import com.agroenvios.clientes.primary.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.repository.query.parser.PartTree;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoServicePaginadoTest {

    private static final long USER_ID = 5L;

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PedidoService pedidoService;

    @BeforeEach
    void usuarioExistente() {
        User user = new User();
        user.setId(USER_ID);
        // lenient: la prueba de nombres de consultas no toca al usuario
        lenient().when(userRepository.findByUsername("jeduardo")).thenReturn(Optional.of(user));
    }

    private static Pedido pedido(long id, String estado) {
        Pedido p = new Pedido();
        p.setId(id);
        p.setEstado(estado);
        p.setItems(List.of());
        return p;
    }

    private Pageable paginaPedida() {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(pedidoRepository).findByUserIdOrderByCreatedAtDescIdDesc(eq(USER_ID), captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("página 0 sin filtro: trae la página, hasMore y los conteos por estado")
    void paginaCeroTraeConteos() {
        when(pedidoRepository.findByUserIdOrderByCreatedAtDescIdDesc(eq(USER_ID), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of(pedido(3, "APROBADO"), pedido(2, "PENDIENTE")), Pageable.ofSize(2), true));
        when(pedidoRepository.contarPorEstado(USER_ID))
                .thenReturn(List.of(new Object[]{"APROBADO", 7L}, new Object[]{"PENDIENTE", 2L}));

        PedidoPageDTO page = pedidoService.getMisPedidosPaginado("jeduardo", null, 0, 2);

        assertEquals(List.of(3L, 2L), page.getItems().stream().map(i -> i.getId()).toList());
        assertTrue(page.isHasMore());
        assertEquals(0, page.getPage());
        assertEquals(7L, page.getConteos().get("APROBADO"));
        assertEquals(2L, page.getConteos().get("PENDIENTE"));
    }

    @Test
    @DisplayName("páginas siguientes no calculan conteos")
    void paginasSiguientesSinConteos() {
        when(pedidoRepository.findByUserIdOrderByCreatedAtDescIdDesc(eq(USER_ID), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of(pedido(1, "APROBADO")), Pageable.ofSize(10), false));

        PedidoPageDTO page = pedidoService.getMisPedidosPaginado("jeduardo", "", 3, 10);

        assertEquals(3, page.getPage());
        assertFalse(page.isHasMore());
        assertNull(page.getConteos());
        verify(pedidoRepository, never()).contarPorEstado(any());
    }

    @Test
    @DisplayName("el filtro de estado se normaliza a mayúsculas y usa la consulta filtrada")
    void filtraPorEstado() {
        when(pedidoRepository.findByUserIdAndEstadoOrderByCreatedAtDescIdDesc(eq(USER_ID), eq("APROBADO"), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of(pedido(9, "APROBADO")), Pageable.ofSize(10), false));

        PedidoPageDTO page = pedidoService.getMisPedidosPaginado("jeduardo", " aprobado ", 1, 10);

        assertEquals(1, page.getItems().size());
        verify(pedidoRepository, never()).findByUserIdOrderByCreatedAtDescIdDesc(any(), any());
    }

    @Test
    @DisplayName("el tamaño de página se acota a 1..50 y la página negativa cuenta como 0")
    void acotaTamanoYPagina() {
        when(pedidoRepository.findByUserIdOrderByCreatedAtDescIdDesc(eq(USER_ID), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of(), Pageable.ofSize(1), false));
        when(pedidoRepository.contarPorEstado(USER_ID)).thenReturn(List.of());

        pedidoService.getMisPedidosPaginado("jeduardo", null, -4, 5000);

        Pageable pageable = paginaPedida();
        assertEquals(50, pageable.getPageSize());
        assertEquals(0, pageable.getPageNumber());
    }

    @Test
    @DisplayName("las consultas derivadas del repositorio apuntan a propiedades reales de Pedido")
    void consultasDerivadasSonValidas() {
        assertDoesNotThrow(() -> new PartTree("findByUserIdOrderByCreatedAtDescIdDesc", Pedido.class));
        assertDoesNotThrow(() -> new PartTree("findByUserIdAndEstadoOrderByCreatedAtDescIdDesc", Pedido.class));
    }
}
