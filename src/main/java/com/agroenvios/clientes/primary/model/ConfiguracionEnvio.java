package com.agroenvios.clientes.primary.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Cabecera del tarifario de envío. Los precios viven en sus {@link TarifaRangoEnvio}
 * (un rango por tramo de distancia). Solo una fila debe estar activa a la vez: es la
 * que usa {@code EnvioService} para cotizar. Se edita directo en la tabla, sin redeploy.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "configuracion_envio")
public class ConfiguracionEnvio extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    /** Solo se usa la configuración activa más reciente. */
    @Column(nullable = false)
    @Builder.Default
    private Boolean activa = true;

    /** Punto de partida de todos los envíos (bodega/centro de distribución). */
    @Column(nullable = false)
    private Double origenLatitud;

    @Column(nullable = false)
    private Double origenLongitud;

    /** Techo de seguridad para la tarifa final. {@code null} = sin tope. */
    @Column(precision = 10, scale = 2)
    private BigDecimal tarifaMaxima;

    /** Tramos de distancia con su tarifa. También definen hasta dónde hay cobertura. */
    @OneToMany(mappedBy = "configuracion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("radioInicialKm ASC")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<TarifaRangoEnvio> rangos = new ArrayList<>();
}
