package com.agroenvios.clientes.primary.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "pedidos")
public class Pedido extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PedidoItem> items;

    @Column(nullable = false)
    private Long direccionId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(nullable = false)
    private String estado; // APROBADO, RECHAZADO, PENDIENTE, CANCELADO

    private String pagoId; // ID del pago en MercadoPago

    @Column(nullable = false)
    private String referenciaPago; // UUID — external_reference enviado a MercadoPago
}
