package com.agroenvios.clientes.primary.dto.pago;

import lombok.Data;

@Data
public class ItemPagoDto {
    private String nombre;
    private int cantidad;
    private double precio;
}
