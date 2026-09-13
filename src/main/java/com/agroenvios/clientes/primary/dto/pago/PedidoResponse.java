package com.agroenvios.clientes.primary.dto.pago;

import com.agroenvios.clientes.primary.model.Pedido;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class PedidoResponse {
    private Long id;
    private String estado;
    private String estadoEntrega;
    private BigDecimal subtotal;
    private BigDecimal tarifaEnvio;
    private BigDecimal total;
    private Long direccionId;
    private String referenciaPago;
    private String pagoId;
    private String codigoEntrega;
    private List<PedidoItemResponse> items;
    private LocalDateTime createdAt;

    public record TradeShopInfo(String nombre, String logoUrl) {}

    // tradeShopById: resuelto aparte (tabla tradeShop vive en la BD de proveedores)
    // para no disparar una consulta por item — ver PedidoService.
    public static PedidoResponse from(Pedido pedido, Map<Long, TradeShopInfo> tradeShopById) {
        PedidoResponse dto = new PedidoResponse();
        dto.setId(pedido.getId());
        dto.setEstado(pedido.getEstado());
        dto.setEstadoEntrega(pedido.getEstadoEntrega());
        dto.setSubtotal(pedido.getSubtotal());
        dto.setTarifaEnvio(pedido.getTarifaEnvio());
        dto.setTotal(pedido.getTotal());
        dto.setDireccionId(pedido.getDireccionId());
        dto.setReferenciaPago(pedido.getReferenciaPago());
        dto.setPagoId(pedido.getPagoId());
        dto.setCodigoEntrega(pedido.getCodigoEntrega());
        dto.setCreatedAt(pedido.getCreatedAt());
        dto.setItems(pedido.getItems().stream()
                .map(item -> {
                    TradeShopInfo info = tradeShopById.get(item.getTradeShopId());
                    return PedidoItemResponse.from(item, info != null ? info.nombre() : null, info != null ? info.logoUrl() : null);
                })
                .toList());
        return dto;
    }
}
