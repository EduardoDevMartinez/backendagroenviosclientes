package com.agroenvios.clientes.dto.pago;

import lombok.Data;

@Data
public class ItemPagoDto {
    private String nombre;
    private int cantidad;
    private double precio;
}
