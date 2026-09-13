package com.agroenvios.clientes.primary.dto.pago;

import com.agroenvios.clientes.primary.model.PedidoItem;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PedidoItemResponse {
    private Long id;
    private String nombre;
    private Double cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
    private Long tradeShopId;
    private String tradeShopNombre;

    public static PedidoItemResponse from(PedidoItem item, String tradeShopNombre) {
        PedidoItemResponse dto = new PedidoItemResponse();
        dto.setId(item.getId());
        dto.setNombre(item.getNombre());
        dto.setCantidad(item.getCantidad());
        dto.setPrecioUnitario(item.getPrecioUnitario());
        dto.setSubtotal(item.getPrecioUnitario().multiply(BigDecimal.valueOf(item.getCantidad())));
        dto.setTradeShopId(item.getTradeShopId());
        dto.setTradeShopNombre(tradeShopNombre);
        return dto;
    }
}
