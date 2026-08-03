package com.agroenvios.clientes.primary.util;

import com.agroenvios.clientes.primary.model.GeocercaPunto;

import java.util.List;

/** Utilidades geométricas para evaluar geocercas sin depender de funciones espaciales de MySQL. */
public final class GeoUtils {

    private static final double RADIO_TIERRA_METROS = 6_371_000d;

    private GeoUtils() {
    }

    /** Distancia en línea recta entre dos coordenadas, en metros (haversine). */
    public static double distanciaMetros(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        return RADIO_TIERRA_METROS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /**
     * Ray casting: determina si el punto cae dentro del polígono formado por los vértices
     * (en el orden dado). El polígono se cierra solo, no hace falta repetir el primer vértice.
     */
    public static boolean dentroDePoligono(double lat, double lng, List<GeocercaPunto> vertices) {
        if (vertices == null || vertices.size() < 3) {
            return false;
        }

        boolean dentro = false;
        int n = vertices.size();

        for (int i = 0, j = n - 1; i < n; j = i++) {
            double latI = vertices.get(i).getLatitud();
            double lngI = vertices.get(i).getLongitud();
            double latJ = vertices.get(j).getLatitud();
            double lngJ = vertices.get(j).getLongitud();

            boolean cruzaLatitud = (latI > lat) != (latJ > lat);
            if (cruzaLatitud) {
                double lngInterseccion = (lngJ - lngI) * (lat - latI) / (latJ - latI) + lngI;
                if (lng < lngInterseccion) {
                    dentro = !dentro;
                }
            }
        }

        return dentro;
    }
}
