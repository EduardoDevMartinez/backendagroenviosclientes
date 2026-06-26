package com.agroenvios.clientes.secondary.repository;

import com.agroenvios.clientes.secondary.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    List<Product> findByActiveTrue();

    List<Product> findByActiveTrueAndAvailableTrue();

    List<Product> findByActiveTrueAndAvailableTrueAndCategoryId(Integer categoryId);

    List<Product> findByActiveTrueAndNameContainingIgnoreCase(String name);
}