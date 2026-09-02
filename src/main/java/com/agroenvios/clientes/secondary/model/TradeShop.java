package com.agroenvios.clientes.secondary.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mapeo de solo lectura de la tabla `tradeShop` (vive en el backend de proveedores,
 * misma base de datos secundaria). Aquí solo se necesita para mostrar el nombre y el
 * logo del comercio dueño de un producto.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tradeShop")
public class TradeShop {

    @Id
    private Long id;

    @Column(name = "nombre_negocio")
    private String nombreNegocio;

    @Column(name = "image_key")
    private String imageKey;
}
