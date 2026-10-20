package com.agroenvios.clientes.secondary.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mapeo de solo lectura de la tabla `day_schedules` (la escribe el backend de proveedores).
 * Un día con is_open = false es de descanso del comercio. `day` guarda el nombre del enum de
 * proveedores: LUNES, MARTES, MIERCOLES, JUEVES, VIERNES, SABADO o DOMINGO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "day_schedules")
public class TradeShopDay {

    @Id
    private Integer id;

    @Column(name = "schedule_id")
    private Integer scheduleId;

    @Column(name = "day")
    private String day;

    @Column(name = "is_open")
    private Boolean isOpen;
}
