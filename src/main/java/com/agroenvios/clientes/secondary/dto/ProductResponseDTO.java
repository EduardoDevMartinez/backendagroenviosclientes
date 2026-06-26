package com.agroenvios.clientes.secondary.dto;

import com.agroenvios.clientes.secondary.model.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDTO {

    private Integer id;
    private String name;
    private String description;
    private BigDecimal retailPrice;
    private BigDecimal wholesalePrice;
    private BigDecimal discountPercentage;
    private Integer stockAvailable;
    private Integer wholesaleQuantity;
    private String unit;
    private String imageUrl;
    private Boolean active;
    private Boolean available;
    private Integer categoryId;
    private String categoryName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductResponseDTO from(Product product, String imageUrl) {
        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .retailPrice(product.getRetailPrice())
                .wholesalePrice(product.getWholesalePrice())
                .discountPercentage(product.getDiscountPercentage())
                .stockAvailable(product.getStockAvailable())
                .wholesaleQuantity(product.getWholesaleQuantity())
                .unit(product.getUnit())
                .imageUrl(imageUrl)
                .active(product.getActive())
                .available(product.getAvailable())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
