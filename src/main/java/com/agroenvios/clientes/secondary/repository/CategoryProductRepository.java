package com.agroenvios.clientes.secondary.repository;

import com.agroenvios.clientes.secondary.model.CategoryProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryProductRepository extends JpaRepository<CategoryProduct, Integer> {

    List<CategoryProduct> findByActiveTrue();
}
