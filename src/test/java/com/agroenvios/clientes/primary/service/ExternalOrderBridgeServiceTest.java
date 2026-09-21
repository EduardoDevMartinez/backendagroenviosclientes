package com.agroenvios.clientes.primary.service;

import com.agroenvios.clientes.primary.model.DireccionEntrega;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExternalOrderBridgeServiceTest {

    private static DireccionEntrega direccion(String calle, String exterior, String interior) {
        return DireccionEntrega.builder()
                .calle(calle)
                .numeroExterior(exterior)
                .numeroInterior(interior)
                .build();
    }

    @Test
    @DisplayName("calle + exterior + interior salen en una sola línea para el fletista")
    void integraExteriorEInterior() {
        assertEquals("Av Juárez 816 Int. 3A", ExternalOrderBridgeService.calleCompleta(direccion("Av Juárez", "816", "3A")));
    }

    @Test
    @DisplayName("sin interior solo agrega el exterior")
    void soloExterior() {
        assertEquals("Av Juárez 816", ExternalOrderBridgeService.calleCompleta(direccion("Av Juárez", "816", null)));
    }

    @Test
    @DisplayName("direcciones anteriores (números null) salen igual que antes: solo la calle")
    void direccionLegacySinNumeros() {
        assertEquals("Av Juárez 816 Int. 3", ExternalOrderBridgeService.calleCompleta(direccion("Av Juárez 816 Int. 3", null, null)));
    }

    @Test
    @DisplayName("números en blanco se ignoran y no dejan espacios de sobra")
    void ignoraNumerosEnBlanco() {
        assertEquals("Camino a la parcela", ExternalOrderBridgeService.calleCompleta(direccion(" Camino a la parcela ", "  ", "")));
    }
}
