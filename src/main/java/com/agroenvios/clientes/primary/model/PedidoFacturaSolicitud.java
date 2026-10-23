package com.agroenvios.clientes.primary.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Solicitud de factura de un cliente a un comercio de su pedido. Cada comercio factura lo
 * suyo, así que hay una por (pedido, comercio) y se puede pedir una sola vez.
 */
@Getter
@Setter
@Entity
@Table(name = "pedido_factura_solicitud",
        uniqueConstraints = @UniqueConstraint(columnNames = {"pedido_id", "trade_shop_id"}))
public class PedidoFacturaSolicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pedido_id", nullable = false)
    private Long pedidoId;

    @Column(name = "trade_shop_id", nullable = false)
    private Long tradeShopId;

    @Column(name = "solicitada_at", nullable = false)
    private LocalDateTime solicitadaAt;
}
