package com.agroenvios.clientes.secondary.repository;

import com.agroenvios.clientes.secondary.model.TradeShop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TradeShopRepository extends JpaRepository<TradeShop, Long> {
}
