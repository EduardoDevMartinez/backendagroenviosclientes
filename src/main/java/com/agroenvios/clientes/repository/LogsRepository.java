package com.agroenvios.clientes.repository;

import com.agroenvios.clientes.model.LogsModel;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface LogsRepository extends JpaRepository<LogsModel, Integer> {

    List<LogsModel> findByFechaBefore(LocalDateTime fecha, Pageable pageable);
}
