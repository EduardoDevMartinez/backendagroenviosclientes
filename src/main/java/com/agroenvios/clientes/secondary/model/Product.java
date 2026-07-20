package com.agroenvios.clientes.secondary.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    private String description;

    private BigDecimal retailPrice;

    private BigDecimal wholesalePrice;

    private BigDecimal discountPercentage;

    private Integer stockAvailable;

    private Integer wholesaleQuantity;

    private String unit;

    @Column(name = "image_key")
    private String imageKey;

    @Column(name = "thumbnail_key")
    private String thumbnailKey;

    private Boolean active;

    private Boolean available;

    // Comercio dueño del producto (tabla TradeShop vive en el backend de proveedores;
    // aquí solo necesitamos el ID para poder armar pedidos que lo referencien)
    @Column(name = "trade_shop_id")
    private Long tradeShopId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private CategoryProduct category;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}