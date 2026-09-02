package com.agroenvios.clientes.secondary.service;

import com.agroenvios.clientes.primary.service.MinioService;
import com.agroenvios.clientes.secondary.dto.CategoryOptionDTO;
import com.agroenvios.clientes.secondary.dto.ProductPageDTO;
import com.agroenvios.clientes.secondary.dto.ProductResponseDTO;
import com.agroenvios.clientes.secondary.model.Product;
import com.agroenvios.clientes.secondary.repository.CategoryProductRepository;
import com.agroenvios.clientes.secondary.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryProductRepository categoryProductRepository;
    private final MinioService minioService;

    @Value("${aws.s3.proveedores-bucket:agroenvios-files}")
    private String proveedoresBucket;

    private ProductResponseDTO toDTO(Product product) {
        String imageUrl = null;
        if (product.getImageKey() != null && !product.getImageKey().isBlank()) {
            imageUrl = minioService.generatePresignedUrl(product.getImageKey(), proveedoresBucket);
        }

        // Si el producto aún no tiene miniatura (subida antes de este feature), usamos
        // la imagen completa como respaldo para no dejar la miniatura vacía.
        String thumbnailUrl = imageUrl;
        if (product.getThumbnailKey() != null && !product.getThumbnailKey().isBlank()) {
            thumbnailUrl = minioService.generatePresignedUrl(product.getThumbnailKey(), proveedoresBucket);
        }

        String comercioNombre = null;
        String comercioLogoUrl = null;
        if (product.getTradeShop() != null) {
            comercioNombre = product.getTradeShop().getNombreNegocio();
            String logoKey = product.getTradeShop().getImageKey();
            if (logoKey != null && !logoKey.isBlank()) {
                comercioLogoUrl = minioService.generatePresignedUrl(logoKey, proveedoresBucket);
            }
        }

        return ProductResponseDTO.from(product, imageUrl, thumbnailUrl, comercioNombre, comercioLogoUrl);
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

    /**
     * Trae productos disponibles en páginas para scroll infinito, con filtro
     * opcional de categoría y búsqueda por nombre resueltos del lado del servidor.
     */
    public ProductPageDTO getAvailablePaged(Integer categoryId, String search, int page, int size) {
        String normalizedSearch = (search == null || search.isBlank()) ? null : search.trim();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        Page<Product> result = productRepository.findAvailablePaged(categoryId, normalizedSearch, pageable);

        List<ProductResponseDTO> items = result.getContent().stream().map(this::toDTO).toList();
        return ProductPageDTO.builder()
                .items(items)
                .hasMore(result.hasNext())
                .page(page)
                .build();
    }

    public List<CategoryOptionDTO> getAvailableCategories() {
        return categoryProductRepository.findByActiveTrue().stream()
                .map(c -> new CategoryOptionDTO(c.getId(), c.getName()))
                .toList();
    }
}
