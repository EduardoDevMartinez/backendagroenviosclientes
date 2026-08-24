package com.agroenvios.clientes.primary.repository;

import com.agroenvios.clientes.primary.model.TarifaRangoEnvio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TarifaRangoEnvioRepository extends JpaRepository<TarifaRangoEnvio, Long> {
}
