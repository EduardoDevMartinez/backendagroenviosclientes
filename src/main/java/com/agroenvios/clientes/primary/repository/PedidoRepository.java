package com.agroenvios.clientes.primary.repository;

import com.agroenvios.clientes.primary.model.Pedido;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Slice (no Page): el scroll infinito solo necesita saber si hay más, así se evita el
    // COUNT. El desempate por id mantiene estable el orden cuando dos pedidos comparten
    // createdAt, para que ninguno se repita ni se salte entre páginas.
    Slice<Pedido> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

    Slice<Pedido> findByUserIdAndEstadoOrderByCreatedAtDescIdDesc(Long userId, String estado, Pageable pageable);

    // Filas [estado, cantidad]
    @Query("SELECT p.estado, COUNT(p) FROM Pedido p WHERE p.user.id = :userId GROUP BY p.estado")
    List<Object[]> contarPorEstado(@Param("userId") Long userId);

    Optional<Pedido> findByIdAndUserId(Long id, Long userId);

    Optional<Pedido> findByReferenciaPago(String referenciaPago);
}
