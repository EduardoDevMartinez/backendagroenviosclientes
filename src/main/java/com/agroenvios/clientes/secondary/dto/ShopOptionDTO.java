package com.agroenvios.clientes.secondary.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Comercio con productos visibles en el catálogo, para el filtro por comercio de la app. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopOptionDTO {
    private Long id;
    private String name;
    private String logoUrl;
    private Long productCount;
}
