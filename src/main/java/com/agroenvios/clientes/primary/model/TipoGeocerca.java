package com.agroenvios.clientes.primary.model;

public enum TipoGeocerca {
    /** Círculo definido por centro (lat/lng) y radio en metros. */
    CIRCULO,
    /** Polígono definido por los vértices de {@code geocerca_envio_puntos}. */
    POLIGONO
}
