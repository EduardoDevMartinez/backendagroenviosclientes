package com.agroenvios.clientes.repository;

import com.agroenvios.clientes.model.DireccionEntrega;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DireccionEntregaRepository extends JpaRepository<DireccionEntrega, Long> {
    List<DireccionEntrega> findByUserId(Long userId);
    Optional<DireccionEntrega> findByUserIdAndEsPrincipalTrue(Long userId);
}
