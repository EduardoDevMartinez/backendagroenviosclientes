package com.agroenvios.clientes.repository;

import com.agroenvios.clientes.model.MetodoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MetodoPagoRepository extends JpaRepository<MetodoPago, Long> {
    List<MetodoPago> findByUserId(Long userId);
    Optional<MetodoPago> findByUserIdAndEsPrincipalTrue(Long userId);
}
