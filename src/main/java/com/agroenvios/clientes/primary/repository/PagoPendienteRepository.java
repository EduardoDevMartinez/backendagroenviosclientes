package com.agroenvios.clientes.primary.repository;

import com.agroenvios.clientes.primary.model.PagoPendiente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoPendienteRepository extends JpaRepository<PagoPendiente, String> {
}
