package com.agroenvios.clientes.secondary.service;

import com.agroenvios.clientes.secondary.model.Product;
import com.agroenvios.clientes.secondary.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public List<Product> getAll() {
        return productRepository.findAll();
    }

    public List<Product> getAvailable() {
        return productRepository.findByActiveTrueAndAvailableTrue();
    }

    public Product getById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con id: " + id));
    }
}
