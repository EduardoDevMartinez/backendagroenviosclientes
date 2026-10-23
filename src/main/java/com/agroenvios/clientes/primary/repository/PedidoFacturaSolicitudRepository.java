package com.agroenvios.clientes.primary.repository;

import com.agroenvios.clientes.primary.model.PedidoFacturaSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PedidoFacturaSolicitudRepository extends JpaRepository<PedidoFacturaSolicitud, Long> {

    List<PedidoFacturaSolicitud> findByPedidoIdIn(Collection<Long> pedidoIds);

    boolean existsByPedidoIdAndTradeShopId(Long pedidoId, Long tradeShopId);
}
