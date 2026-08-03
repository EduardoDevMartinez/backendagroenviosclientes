package com.agroenvios.clientes.primary.repository;

import com.agroenvios.clientes.primary.model.GeocercaEnvio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GeocercaEnvioRepository extends JpaRepository<GeocercaEnvio, Long> {

    /** Geocercas activas con sus vértices, de mayor a menor prioridad. */
    @Query("""
            SELECT DISTINCT g FROM GeocercaEnvio g
            LEFT JOIN FETCH g.puntos
            WHERE g.activa = true
            ORDER BY g.prioridad DESC, g.id ASC
            """)
    List<GeocercaEnvio> findActivasConPuntos();
}
