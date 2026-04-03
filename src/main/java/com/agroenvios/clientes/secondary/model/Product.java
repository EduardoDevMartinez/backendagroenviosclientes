package com.agroenvios.clientes.secondary.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Boolean active;

    private Boolean available;

    private LocalDate createdAt;

    private String description;

    private String name;

    private Double retailPrice;

    private Integer stockAvailable;

    private String unit;
}