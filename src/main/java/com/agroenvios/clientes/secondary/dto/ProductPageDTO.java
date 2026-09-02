package com.agroenvios.clientes.secondary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductPageDTO {
    private List<ProductResponseDTO> items;
    private boolean hasMore;
    private int page;
}
