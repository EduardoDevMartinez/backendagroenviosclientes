package com.agroenvios.clientes.secondary.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DiasDescansoTest {

    @Test
    void nombraLosDiasComoLosGuardaProveedores() {
        // 2026-10-19 es lunes
        assertEquals("LUNES", DiasDescanso.nombre(LocalDate.of(2026, 10, 19)));
        assertEquals("MARTES", DiasDescanso.nombre(LocalDate.of(2026, 10, 20)));
        assertEquals("MIERCOLES", DiasDescanso.nombre(LocalDate.of(2026, 10, 21)));
        assertEquals("JUEVES", DiasDescanso.nombre(LocalDate.of(2026, 10, 22)));
        assertEquals("VIERNES", DiasDescanso.nombre(LocalDate.of(2026, 10, 23)));
        assertEquals("SABADO", DiasDescanso.nombre(LocalDate.of(2026, 10, 24)));
        assertEquals("DOMINGO", DiasDescanso.nombre(LocalDate.of(2026, 10, 25)));
    }
}
