package com.agroenvios.clientes.primary.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Rango de distancia con su propia tarifa. Se elige el rango donde cae la distancia
 * de la ruta y se cobra:
 *
 * <pre>
 *   tarifaBase + (distanciaKm * costoPorKm) + (tiempoMinutos * costoPorMinuto)
 * </pre>
 *
 * Ejemplo: rango 0–10 km con base $120 y $20/km → una entrega de 9 km cuesta
 * 120 + (9 × 20) = $300.
 *
 * <p>Los rangos también definen la cobertura: si la distancia no cae en ninguno,
 * no se entrega.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tarifas_envio_rango")
public class TarifaRangoEnvio extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "configuracion_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ConfiguracionEnvio configuracion;

    /** Etiqueta para identificarlo en la tabla, ej. "Zona local 0–10 km". */
    private String nombre;

    /** Desde (inclusive), en km de ruta. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal radioInicialKm;

    /** Hasta (inclusive), en km de ruta. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal radioFinalKm;

    /** Tarifa base mínima del rango: se cobra siempre que aplique este rango. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal tarifaBase;

    /** Se multiplica por la distancia total de la ruta. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal costoPorKm;

    /** Se multiplica por el tiempo total de la ruta. 0 = no se cobra el tiempo. */
    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal costoPorMinuto = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activa = true;
}
