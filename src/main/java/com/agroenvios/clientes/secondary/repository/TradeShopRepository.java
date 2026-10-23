package com.agroenvios.clientes.secondary.repository;

import com.agroenvios.clientes.secondary.model.TradeShop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradeShopRepository extends JpaRepository<TradeShop, Long> {

    // El comercio no guarda correo propio: el contacto es el de la cuenta (tabla `users` de
    // proveedores) ligada a él. Solo lectura; devuelve el correo de la cuenta más antigua.
    @Query(value = "SELECT correo FROM users WHERE trade_shop_id = :tradeShopId "
            + "AND correo IS NOT NULL AND correo <> '' ORDER BY id LIMIT 1", nativeQuery = true)
    List<String> findCorreoContacto(@Param("tradeShopId") Long tradeShopId);
}
