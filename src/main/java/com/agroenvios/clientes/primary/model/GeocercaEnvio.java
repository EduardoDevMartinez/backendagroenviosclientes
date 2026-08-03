package com.agroenvios.clientes.primary.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Zona geográfica que modifica la tarifa cuando la dirección de entrega cae dentro.
 * Puede ser un círculo (centro + radio) o un polígono ({@link GeocercaPunto}).
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "geocercas_envio")
public class GeocercaEnvio extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoGeocerca tipo;

    /** Factor sobre el subtotal (base + distancia + tiempo). 1.000 = sin cambio. */
    @Column(nullable = false, precision = 6, scale = 3)
    @Builder.Default
    private BigDecimal multiplicador = BigDecimal.ONE;

    /** Cargo fijo extra que se suma después de aplicar el multiplicador. */
    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal recargoFijo = BigDecimal.ZERO;

    /** Mayor prioridad = se evalúa primero y gana cuando es exclusiva. */
    @Column(nullable = false)
    @Builder.Default
    private Integer prioridad = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activa = true;

    /** Si aplica, ignora al resto de geocercas que también coincidan. */
    @Column(nullable = false)
    @Builder.Default
    private Boolean exclusiva = false;

    /** Zona sin cobertura: si la dirección cae aquí, no se puede cotizar. */
    @Column(nullable = false)
    @Builder.Default
    private Boolean bloqueaEnvio = false;

    // ── Solo para tipo CIRCULO ────────────────────────────────────────────────
    private Double centroLatitud;

    private Double centroLongitud;

    private Double radioMetros;

    // ── Solo para tipo POLIGONO ───────────────────────────────────────────────
    @OneToMany(mappedBy = "geocerca", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<GeocercaPunto> puntos = new ArrayList<>();
}
