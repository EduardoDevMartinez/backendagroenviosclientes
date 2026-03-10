package com.agroenvios.clientes.primary.repository;

import com.agroenvios.clientes.primary.model.LogsModel;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface LogsRepository extends JpaRepository<LogsModel, Integer> {

    List<LogsModel> findByFechaBefore(LocalDateTime fecha, Pageable pageable);
}
