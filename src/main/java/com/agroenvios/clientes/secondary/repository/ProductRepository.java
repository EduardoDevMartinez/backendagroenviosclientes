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
            "AND (t IS NULL OR t.isOpen IS NULL OR t.isOpen = true) " +
            "AND (t IS NULL OR t.scheduleId IS NULL OR NOT EXISTS (SELECT d.id FROM TradeShopDay d " +
            "WHERE d.scheduleId = t.scheduleId AND d.day = :manana AND d.isOpen = false))")
    List<Product> findInStock(@Param("manana") String manana);

    @Query("SELECT p FROM Product p LEFT JOIN p.tradeShop t WHERE p.active = true AND p.available = true AND p.stockAvailable > 0 " +
            "AND (t IS NULL OR t.isOpen IS NULL OR t.isOpen = true) " +
            "AND (t IS NULL OR t.scheduleId IS NULL OR NOT EXISTS (SELECT d.id FROM TradeShopDay d " +
            "WHERE d.scheduleId = t.scheduleId AND d.day = :manana AND d.isOpen = false)) " +
            "AND p.category.id = :categoryId")
    List<Product> findInStockByCategoryId(@Param("categoryId") Integer categoryId,
                                           @Param("manana") String manana);

    List<Product> findByActiveTrueAndNameContainingIgnoreCase(String name);

    @Query("SELECT p FROM Product p LEFT JOIN p.tradeShop t WHERE p.active = true AND p.available = true AND p.stockAvailable > 0 " +
            "AND (t IS NULL OR t.isOpen IS NULL OR t.isOpen = true) " +
            "AND (t IS NULL OR t.scheduleId IS NULL OR NOT EXISTS (SELECT d.id FROM TradeShopDay d " +
            "WHERE d.scheduleId = t.scheduleId AND d.day = :manana AND d.isOpen = false)) " +
            "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
            "AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Product> findAvailablePaged(@Param("categoryId") Integer categoryId,
                                      @Param("search") String search,
                                      @Param("manana") String manana,
                                      Pageable pageable);

    /**
     * Misma regla de catálogo que findAvailablePaged pero sin paginar ni filtrar por texto:
     * devuelve (id, tradeShopId, nombre, comercio, categoría, descripción) para que el servicio ordene y
     * busque por palabras sin acentos.
     */
    @Query("SELECT p.id, p.tradeShopId, p.name, t.nombreNegocio, c.name, p.description FROM Product p " +
            "LEFT JOIN p.tradeShop t LEFT JOIN p.category c " +
            "WHERE p.active = true AND p.available = true AND p.stockAvailable > 0 " +
            "AND (t IS NULL OR t.isOpen IS NULL OR t.isOpen = true) " +
            "AND (t IS NULL OR t.scheduleId IS NULL OR NOT EXISTS (SELECT d.id FROM TradeShopDay d " +
            "WHERE d.scheduleId = t.scheduleId AND d.day = :manana AND d.isOpen = false)) " +
            "AND (:categoryId IS NULL OR c.id = :categoryId)")
    List<Object[]> findAvailableForSearch(@Param("categoryId") Integer categoryId,
                                           @Param("manana") String manana);

    /** Categorías con al menos un producto visible en el catálogo y cuántos tienen, de más a menos. */
    @Query("SELECT c.id, c.name, COUNT(p) FROM Product p " +
            "LEFT JOIN p.tradeShop t JOIN p.category c " +
            "WHERE p.active = true AND p.available = true AND p.stockAvailable > 0 " +
            "AND (t IS NULL OR t.isOpen IS NULL OR t.isOpen = true) " +
            "AND (t IS NULL OR t.scheduleId IS NULL OR NOT EXISTS (SELECT d.id FROM TradeShopDay d " +
            "WHERE d.scheduleId = t.scheduleId AND d.day = :manana AND d.isOpen = false)) " +
            "AND c.active = true " +
            "GROUP BY c.id, c.name ORDER BY COUNT(p) DESC, c.name ASC")
    List<Object[]> countAvailableByCategory(@Param("manana") String manana);

    /** Comercios con al menos un producto visible en el catálogo y cuántos tienen, de más a menos. */
    @Query("SELECT t.id, t.nombreNegocio, t.imageKey, COUNT(p) FROM Product p " +
            "JOIN p.tradeShop t " +
            "WHERE p.active = true AND p.available = true AND p.stockAvailable > 0 " +
            "AND (t.isOpen IS NULL OR t.isOpen = true) " +
            "AND (t.scheduleId IS NULL OR NOT EXISTS (SELECT d.id FROM TradeShopDay d " +
            "WHERE d.scheduleId = t.scheduleId AND d.day = :manana AND d.isOpen = false)) " +
            "GROUP BY t.id, t.nombreNegocio, t.imageKey ORDER BY COUNT(p) DESC, t.nombreNegocio ASC")
    List<Object[]> countAvailableByShop(@Param("manana") String manana);
}
