package com.agroenvios.clientes.secondary.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Cómo se incrementa la cantidad al comprar un producto de esta unidad. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductUnitDTO {
    private String code;
    private String name;
    private BigDecimal step;
    private BigDecimal minQuantity;
}
