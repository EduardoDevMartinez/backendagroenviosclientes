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

    private Boolean active;

    private Boolean available;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private CategoryProduct category;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}