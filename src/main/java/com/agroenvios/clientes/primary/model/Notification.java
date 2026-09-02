package com.agroenvios.clientes.primary.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notif_user_read", columnList = "user_id,is_read"),
        @Index(name = "idx_notif_created_at", columnList = "created_at")
})
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Mismo valor de estado que dispara la notificación push (APROBADO, RECHAZADO,
    // IN_DELIVERY, DELIVERED, etc.), para que el frontend elija icono/color.
    @Column(nullable = false)
    private String estado;

    @Column(nullable = false)
    private String title;

    @Column(length = 500, nullable = false)
    private String message;

    @Column(name = "pedido_id")
    private Long pedidoId;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;
}
