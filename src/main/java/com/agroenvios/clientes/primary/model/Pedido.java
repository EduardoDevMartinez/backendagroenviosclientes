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
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal tarifaEnvio;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    // Comisión que Mercado Pago retiene automáticamente del total antes de depositar
    // a Agroenvios. Informativa: nunca se resta de lo que se le debe al comercio ni al
    // fletista, solo se usa para que Agroenvios lleve el control de su costo real.
    @Column(precision = 10, scale = 2)
    private BigDecimal comisionMercadoPago;

    @Column(nullable = false)
    private String estado; // APROBADO, RECHAZADO, PENDIENTE, CANCELADO

    // Progreso del pedido del lado de proveedores (PENDING, REVIEWING,
    // WAITING_CUSTOMER_ACTION, PARTIALLY_FULFILLED, FULFILLED, IN_DELIVERY, DELIVERED,
    // CANCELLED). Null hasta que proveedores manda la primera actualización — pedidos
    // que nunca se replicaron allá (sin productId/tradeShopId) se quedan así.
    @Column(name = "estado_entrega")
    private String estadoEntrega;

    private String pagoId; // ID del pago en MercadoPago

    @Column(nullable = false)
    private String referenciaPago; // UUID — external_reference enviado a MercadoPago
}
