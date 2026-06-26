package com.agroenvios.clientes.secondary.service;

import com.agroenvios.clientes.primary.service.MinioService;
import com.agroenvios.clientes.secondary.dto.ProductResponseDTO;
import com.agroenvios.clientes.secondary.model.Product;
import com.agroenvios.clientes.secondary.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final MinioService minioService;

    @Value("${aws.s3.proveedores-bucket:agroenvios-files}")
    private String proveedoresBucket;

    private ProductResponseDTO toDTO(Product product) {
        String imageUrl = null;
        if (product.getImageKey() != null && !product.getImageKey().isBlank()) {
            imageUrl = minioService.generatePresignedUrl(product.getImageKey(), proveedoresBucket);
        }
        return ProductResponseDTO.from(product, imageUrl);
    }

    public List<ProductResponseDTO> getAll() {
        return productRepository.findAll().stream().map(this::toDTO).toList();
    }

    public List<ProductResponseDTO> getAvailable() {
        return productRepository.findByActiveTrueAndAvailableTrue().stream().map(this::toDTO).toList();
    }

    public ProductResponseDTO getById(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con id: " + id));
        return toDTO(product);
    }

    public List<ProductResponseDTO> getByCategory(Integer categoryId) {
        return productRepository.findByActiveTrueAndAvailableTrueAndCategoryId(categoryId)
                .stream().map(this::toDTO).toList();
    }

    public List<ProductResponseDTO> searchByName(String name) {
        return productRepository.findByActiveTrueAndNameContainingIgnoreCase(name)
                .stream().map(this::toDTO).toList();
    }
}
