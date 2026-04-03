package com.agroenvios.clientes.primary.dto.pago;

import com.agroenvios.clientes.primary.model.Pedido;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PedidoResponse {
    private Long id;
    private String estado;
    private BigDecimal total;
    private Long direccionId;
    private String referenciaPago;
    private String pagoId;
    private List<PedidoItemResponse> items;
    private LocalDateTime createdAt;

    public static PedidoResponse from(Pedido pedido) {
        PedidoResponse dto = new PedidoResponse();
        dto.setId(pedido.getId());
        dto.setEstado(pedido.getEstado());
        dto.setTotal(pedido.getTotal());
        dto.setDireccionId(pedido.getDireccionId());
        dto.setReferenciaPago(pedido.getReferenciaPago());
        dto.setPagoId(pedido.getPagoId());
        dto.setCreatedAt(pedido.getCreatedAt());
        dto.setItems(pedido.getItems().stream()
                .map(PedidoItemResponse::from)
                .toList());
        return dto;
    }
}
