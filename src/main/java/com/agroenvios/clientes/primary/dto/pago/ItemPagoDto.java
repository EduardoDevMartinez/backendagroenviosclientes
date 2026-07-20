package com.agroenvios.clientes.primary.dto.pago;

import lombok.Data;

@Data
public class ItemPagoDto {
    private String nombre;
    private double cantidad;
    private double precio;

    // Referencia al producto/comercio real (tabla compartida con proveedores), para
    // poder reconstruir el pedido allá cuando se apruebe el pago
    private Long productId;
    private Long tradeShopId;
}
