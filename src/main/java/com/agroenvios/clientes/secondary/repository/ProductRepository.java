package com.agroenvios.clientes.secondary.repository;

import com.agroenvios.clientes.secondary.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * En el catálogo un producto es visible solo si está activo, marcado como disponible por
 * su comercio y tiene stock (> 0). El stock nulo cuenta como agotado, igual que lo trata
 * el backend de proveedores al descontar inventario. Todas las consultas de catálogo
 * comparten esa regla; las demás (findByActiveTrue, búsqueda por nombre) no son del catálogo.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    List<Product> findByActiveTrue();

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.available = true AND p.stockAvailable > 0")
    List<Product> findInStock();

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.available = true AND p.stockAvailable > 0 " +
            "AND p.category.id = :categoryId")
    List<Product> findInStockByCategoryId(@Param("categoryId") Integer categoryId);

    List<Product> findByActiveTrueAndNameContainingIgnoreCase(String name);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.available = true AND p.stockAvailable > 0 " +
            "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
            "AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Product> findAvailablePaged(@Param("categoryId") Integer categoryId,
                                      @Param("search") String search,
                                      Pageable pageable);
}
