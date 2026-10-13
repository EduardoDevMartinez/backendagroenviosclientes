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
 * su comercio, su comercio está abierto y tiene stock (> 0). El stock nulo cuenta como agotado, igual que lo trata
 * el backend de proveedores al descontar inventario. Todas las consultas de catálogo
 * comparten esa regla; las demás (findByActiveTrue, búsqueda por nombre) no son del catálogo.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    List<Product> findByActiveTrue();

    @Query("SELECT p FROM Product p LEFT JOIN p.tradeShop t WHERE p.active = true AND p.available = true AND p.stockAvailable > 0 " +
            "AND (t IS NULL OR t.isOpen IS NULL OR t.isOpen = true)")
    List<Product> findInStock();

    @Query("SELECT p FROM Product p LEFT JOIN p.tradeShop t WHERE p.active = true AND p.available = true AND p.stockAvailable > 0 " +
            "AND (t IS NULL OR t.isOpen IS NULL OR t.isOpen = true) " +
            "AND p.category.id = :categoryId")
    List<Product> findInStockByCategoryId(@Param("categoryId") Integer categoryId);

    List<Product> findByActiveTrueAndNameContainingIgnoreCase(String name);

    @Query("SELECT p FROM Product p LEFT JOIN p.tradeShop t WHERE p.active = true AND p.available = true AND p.stockAvailable > 0 " +
            "AND (t IS NULL OR t.isOpen IS NULL OR t.isOpen = true) " +
            "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
            "AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Product> findAvailablePaged(@Param("categoryId") Integer categoryId,
                                      @Param("search") String search,
                                      Pageable pageable);

    /**
     * Misma regla de catálogo que findAvailablePaged pero sin paginar ni filtrar por texto:
     * devuelve (id, tradeShopId, nombre, comercio, categoría) para que el servicio ordene y
     * busque por palabras sin acentos.
     */
    @Query("SELECT p.id, p.tradeShopId, p.name, t.nombreNegocio, c.name FROM Product p " +
            "LEFT JOIN p.tradeShop t LEFT JOIN p.category c " +
            "WHERE p.active = true AND p.available = true AND p.stockAvailable > 0 " +
            "AND (t IS NULL OR t.isOpen IS NULL OR t.isOpen = true) " +
            "AND (:categoryId IS NULL OR c.id = :categoryId)")
    List<Object[]> findAvailableForSearch(@Param("categoryId") Integer categoryId);
}
