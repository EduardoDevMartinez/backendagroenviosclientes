package com.agroenvios.clientes.primary.repository;

import com.agroenvios.clientes.primary.model.ConfiguracionEnvio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfiguracionEnvioRepository extends JpaRepository<ConfiguracionEnvio, Long> {

    Optional<ConfiguracionEnvio> findFirstByActivaTrueOrderByIdDesc();

    /** Tarifario activo con sus rangos ya cargados, listo para cotizar. */
    @Query("""
            SELECT c FROM ConfiguracionEnvio c
            LEFT JOIN FETCH c.rangos
            WHERE c.activa = true AND c.id = (
                SELECT MAX(c2.id) FROM ConfiguracionEnvio c2 WHERE c2.activa = true
            )
            """)
    Optional<ConfiguracionEnvio> findActivaConRangos();
}
