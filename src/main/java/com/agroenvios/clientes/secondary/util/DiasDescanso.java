package com.agroenvios.clientes.secondary.util;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Regla de descanso de los comercios: los pedidos se entregan al día siguiente, así que los
 * productos de un comercio se ofrecen solo si MAÑANA abre. El día anterior a su descanso no
 * aparecen; el propio día de descanso sí (se entregan al día que reabre).
 */
public final class DiasDescanso {

    public static final ZoneId ZONA = ZoneId.of("America/Mexico_City");

    private DiasDescanso() {
    }

    /** Nombre (LUNES...DOMINGO) del día de mañana en la zona del negocio, como se guarda en day_schedules. */
    public static String diaDeManana() {
        return nombre(LocalDate.now(ZONA).plusDays(1));
    }

    static String nombre(LocalDate fecha) {
        return switch (fecha.getDayOfWeek()) {
            case MONDAY -> "LUNES";
            case TUESDAY -> "MARTES";
            case WEDNESDAY -> "MIERCOLES";
            case THURSDAY -> "JUEVES";
            case FRIDAY -> "VIERNES";
            case SATURDAY -> "SABADO";
            case SUNDAY -> "DOMINGO";
        };
    }
}
