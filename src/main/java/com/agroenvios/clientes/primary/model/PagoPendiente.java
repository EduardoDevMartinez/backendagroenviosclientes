package com.agroenvios.clientes.primary.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Sesión de pago temporal.
 * Se crea cuando el usuario inicia el checkout y se marca como PROCESADO
 * cuando el webhook de MercadoPago confirma el pago.
 * El ID es el UUID que se envía como external_reference a MercadoPago.
 */
@Getter
@Setter
@Entity
@Table(name = "pagos_pendientes")
public class PagoPendiente extends BaseEntity {

    @Id
    private String id; // UUID — usado como external_reference en MercadoPago

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String itemsJson; // JSON serializado de List<ItemPagoDto>

    @Column(nullable = false)
    private Long direccionId;

    @Column(nullable = false)
    private String estado; // PENDIENTE, PROCESADO, RECHAZADO

    @Column(nullable = false)
    private LocalDateTime expiresAt;
}
