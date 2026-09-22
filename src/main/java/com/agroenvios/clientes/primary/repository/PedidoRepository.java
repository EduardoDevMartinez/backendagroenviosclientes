package com.agroenvios.clientes.primary.repository;

import com.agroenvios.clientes.primary.model.Pedido;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    // Pedidos aprobados que proveedores todavía no confirma tener: sin marca de réplica y sin
    // ningún estado de vuelta (proveedores manda REVIEWING en cuanto crea la orden). El usuario
    // y los items vienen ya cargados porque el reintento los usa fuera de cualquier sesión.
    // Sin límite en SQL a propósito: con JOIN FETCH de una colección Hibernate paginaría en memoria.
    @Query("""
            SELECT DISTINCT p FROM Pedido p
            JOIN FETCH p.user
            LEFT JOIN FETCH p.items
            WHERE p.estado = 'APROBADO'
              AND p.replicadoProveedoresAt IS NULL
              AND p.estadoEntrega IS NULL
              AND p.intentosReplicacion < :maxIntentos
              AND p.createdAt >= :desde
              AND p.createdAt <= :hasta
            ORDER BY p.createdAt ASC
            """)
    List<Pedido> findPendientesDeReplicar(@Param("desde") LocalDateTime desde,
                                          @Param("hasta") LocalDateTime hasta,
                                          @Param("maxIntentos") int maxIntentos);

    // UPDATE directos (no save): el envío corre en otro hilo, sin la entidad cargada, y no
    // deben pisar lo que proveedores escribe en paralelo (estado_entrega vía webhook).
    @Modifying
    @Transactional
    @Query("UPDATE Pedido p SET p.replicadoProveedoresAt = :ahora, p.ultimoIntentoReplicacionAt = :ahora, "
            + "p.ultimoErrorReplicacion = null WHERE p.id = :id")
    int marcarReplicado(@Param("id") Long id, @Param("ahora") LocalDateTime ahora);

    @Modifying
    @Transactional
    @Query("UPDATE Pedido p SET p.intentosReplicacion = p.intentosReplicacion + 1, "
            + "p.ultimoIntentoReplicacionAt = :ahora, p.ultimoErrorReplicacion = :error WHERE p.id = :id")
    int registrarFalloReplicacion(@Param("id") Long id, @Param("ahora") LocalDateTime ahora,
                                  @Param("error") String error);

    // Para pedidos que no se pueden replicar nunca (sin productId/tradeShopId): se agotan los
    // intentos de una vez en vez de reintentar para siempre.
    @Modifying
    @Transactional
    @Query("UPDATE Pedido p SET p.intentosReplicacion = :maxIntentos, p.ultimoIntentoReplicacionAt = :ahora, "
            + "p.ultimoErrorReplicacion = :error WHERE p.id = :id")
    int descartarReplicacion(@Param("id") Long id, @Param("ahora") LocalDateTime ahora,
                             @Param("error") String error, @Param("maxIntentos") int maxIntentos);
}
