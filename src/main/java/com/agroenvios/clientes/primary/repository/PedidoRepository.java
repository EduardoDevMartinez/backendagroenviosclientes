package com.agroenvios.clientes.primary.repository;

import com.agroenvios.clientes.primary.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Pedido> findByIdAndUserId(Long id, Long userId);
}
