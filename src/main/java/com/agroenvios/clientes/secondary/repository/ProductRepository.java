package com.agroenvios.clientes.secondary.repository;

import com.agroenvios.clientes.secondary.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    List<Product> findByActiveTrue();

    List<Product> findByActiveTrueAndAvailableTrue();

    List<Product> findByActiveTrueAndAvailableTrueAndCategoryId(Integer categoryId);

    List<Product> findByActiveTrueAndNameContainingIgnoreCase(String name);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.available = true " +
            "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
            "AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Product> findAvailablePaged(@Param("categoryId") Integer categoryId,
                                      @Param("search") String search,
                                      Pageable pageable);
}