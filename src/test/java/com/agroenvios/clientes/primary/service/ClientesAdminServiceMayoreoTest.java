package com.agroenvios.clientes.primary.service;

import com.agroenvios.clientes.primary.model.User;
import com.agroenvios.clientes.primary.repository.DireccionEntregaRepository;
import com.agroenvios.clientes.primary.repository.PedidoRepository;
import com.agroenvios.clientes.primary.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** El admin activa o quita el mayoreo fijo de un cliente. */
@ExtendWith(MockitoExtension.class)
class ClientesAdminServiceMayoreoTest {

    @Mock UserRepository userRepository;
    @Mock PedidoRepository pedidoRepository;
    @Mock DireccionEntregaRepository direccionEntregaRepository;

    @InjectMocks ClientesAdminService service;

    @Test
    void activaElMayoreoFijoDelCliente() {
        User cliente = User.builder().id(7L).username("ana").build();
        when(userRepository.findAll()).thenReturn(List.of(cliente));

        Optional<Boolean> resultado = service.actualizarMayoreoFijo(7L, true);

        assertEquals(Optional.of(true), resultado);
        assertTrue(cliente.getMayoreoFijo());
        verify(userRepository).save(cliente);
    }

    @Test
    void quitaElMayoreoFijoDelCliente() {
        User cliente = User.builder().id(7L).username("ana").mayoreoFijo(true).build();
        when(userRepository.findAll()).thenReturn(List.of(cliente));

        assertEquals(Optional.of(false), service.actualizarMayoreoFijo(7L, false));
        assertFalse(cliente.getMayoreoFijo());
    }

    @Test
    void clienteInexistenteNoCambiaNada() {
        when(userRepository.findAll()).thenReturn(List.of(User.builder().id(1L).username("otro").build()));

        assertTrue(service.actualizarMayoreoFijo(99L, true).isEmpty());
    }
}
