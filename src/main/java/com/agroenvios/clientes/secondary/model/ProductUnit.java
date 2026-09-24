package com.agroenvios.clientes.secondary.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Unidad de venta (kg, g, pieza...) con su incremento. La tabla la crea y administra
 * backendAgroBasicos (ProductUnitModel, panel admin); aquí solo se lee. `code` es lo que
 * trae Product.unit.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "product_units")
public class ProductUnit {

    @Id
    private Integer id;

    private String code;

    private String name;

    private BigDecimal step;

    private BigDecimal minQuantity;

    private Boolean active;
}
