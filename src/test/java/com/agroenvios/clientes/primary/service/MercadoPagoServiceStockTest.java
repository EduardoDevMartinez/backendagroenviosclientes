package com.agroenvios.clientes.primary.service;

import com.agroenvios.clientes.primary.dto.pago.ItemPagoDto;
import com.agroenvios.clientes.primary.repository.PagoPendienteRepository;
import com.agroenvios.clientes.primary.repository.UserRepository;
import com.agroenvios.clientes.secondary.model.Product;
import com.agroenvios.clientes.secondary.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * Antes de cobrar se valida contra el stock actual que lo pedido todavía se pueda surtir.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MercadoPagoServiceStockTest {

    @Mock RestTemplate restTemplate;
    @Mock PagoPendienteRepository pagoPendienteRepository;
    @Mock UserRepository userRepository;
    @Mock ObjectMapper objectMapper;
    @Mock EnvioService envioService;
    @Mock ProductRepository productRepository;

    @InjectMocks MercadoPagoService service;

    @Test
    void conStockSuficienteNoRechaza() {
        catalogo(producto(11, "Cargador", 5, true, true));

        assertDoesNotThrow(() -> service.validarStock(List.of(item(11L, "Cargador", 5))));
    }

    @Test
    void pedirMasDeLoQueHayRechazaConLaCantidadQueQueda() {
        catalogo(producto(11, "Cargador", 2, true, true));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.validarStock(List.of(item(11L, "Cargador", 3))));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertTrue(error.getReason().contains("Cargador (solo quedan 2)"), error.getReason());
    }

    @Test
    void productoAgotadoRechaza() {
        catalogo(producto(11, "Cargador", 0, true, true));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.validarStock(List.of(item(11L, "Cargador", 1))));

        assertTrue(error.getReason().contains("Cargador (agotado)"), error.getReason());
    }

    @Test
    void stockNuloCuentaComoAgotado() {
        catalogo(producto(11, "Cargador", null, true, true));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.validarStock(List.of(item(11L, "Cargador", 1))));

        assertTrue(error.getReason().contains("agotado"), error.getReason());
    }

    @Test
    void productoDesactivadoORetiradoRechaza() {
        catalogo(producto(11, "Cargador", 9, true, false),
                 producto(12, "Florero", 9, false, true));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.validarStock(List.of(item(11L, "Cargador", 1), item(12L, "Florero", 1))));

        assertTrue(error.getReason().contains("Cargador (ya no está disponible)"), error.getReason());
        assertTrue(error.getReason().contains("Florero (ya no está disponible)"), error.getReason());
    }

    @Test
    void productoQueYaNoExisteRechaza() {
        catalogo(); // el catálogo no devuelve nada

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.validarStock(List.of(item(99L, "Fantasma", 1))));

        assertTrue(error.getReason().contains("Fantasma (ya no está disponible)"), error.getReason());
    }

    @Test
    void sumaLasLineasDelMismoProducto() {
        catalogo(producto(11, "Cargador", 3, true, true));

        // 2 + 2 = 4 > 3 aunque cada línea por separado sí alcanzaba
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.validarStock(List.of(item(11L, "Cargador", 2), item(11L, "Cargador", 2))));

        assertTrue(error.getReason().contains("Cargador (solo quedan 3)"), error.getReason());
    }

    @Test
    void cantidadFraccionariaSeCompararConElStockExacto() {
        catalogo(producto(11, "Tomate", 2, true, true));
        assertThrows(ResponseStatusException.class,
                () -> service.validarStock(List.of(item(11L, "Tomate", 2.5))));

        catalogo(producto(11, "Tomate", 3, true, true));
        assertDoesNotThrow(() -> service.validarStock(List.of(item(11L, "Tomate", 2.5))));
    }

    @Test
    void reportaTodosLosProductosConProblema() {
        catalogo(producto(11, "Cargador", 1, true, true),
                 producto(12, "Florero", 0, true, true),
                 producto(13, "Humidificador", 9, true, true));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.validarStock(List.of(
                        item(11L, "Cargador", 4), item(12L, "Florero", 1), item(13L, "Humidificador", 2))));

        assertTrue(error.getReason().contains("Cargador (solo quedan 1)"), error.getReason());
        assertTrue(error.getReason().contains("Florero (agotado)"), error.getReason());
        assertFalse(error.getReason().contains("Humidificador"), error.getReason());
    }

    @Test
    void itemsSinReferenciaAlProductoSeIgnoran() {
        // Carritos viejos que no guardaban productId: no hay contra qué validar
        assertDoesNotThrow(() -> service.validarStock(List.of(item(null, "Antiguo", 1))));
    }

    // ─── Auxiliares ───────────────────────────────────────────────────────────────

    private void catalogo(Product... productos) {
        when(productRepository.findAllById(anyList())).thenReturn(List.of(productos));
    }

    private Product producto(int id, String nombre, Integer stock, boolean activo, boolean disponible) {
        Product product = new Product();
        product.setId(id);
        product.setName(nombre);
        product.setStockAvailable(stock);
        product.setActive(activo);
        product.setAvailable(disponible);
        return product;
    }

    private ItemPagoDto item(Long productId, String nombre, double cantidad) {
        ItemPagoDto item = new ItemPagoDto();
        item.setProductId(productId);
        item.setNombre(nombre);
        item.setCantidad(cantidad);
        item.setPrecio(100);
        return item;
    }
}
