package com.agroenvios.clientes.secondary.controller;

import com.agroenvios.clientes.secondary.dto.CategoryOptionDTO;
import com.agroenvios.clientes.secondary.dto.ProductPageDTO;
import com.agroenvios.clientes.secondary.dto.ProductResponseDTO;
import com.agroenvios.clientes.secondary.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<ProductResponseDTO>> getAll() {
        return ResponseEntity.ok(productService.getAll());
    }

    /**
     * Productos disponibles en páginas para scroll infinito. categoryId y search
     * son opcionales y se resuelven del lado del servidor (no solo en la página cargada).
     */
    @GetMapping("/available")
    public ResponseEntity<ProductPageDTO> getAvailable(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "18") int size,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(productService.getAvailablePaged(categoryId, search, page, size));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryOptionDTO>> getAvailableCategories() {
        return ResponseEntity.ok(productService.getAvailableCategories());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductResponseDTO>> getByCategory(@PathVariable Integer categoryId) {
        return ResponseEntity.ok(productService.getByCategory(categoryId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductResponseDTO>> searchByName(@RequestParam String name) {
        return ResponseEntity.ok(productService.searchByName(name));
    }
}
