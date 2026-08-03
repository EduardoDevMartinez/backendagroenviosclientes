package com.agroenvios.clientes.primary.model;

import jakarta.persistence.*;
import lombok.*;

/** Vértice de una geocerca de tipo POLIGONO. El orden define el contorno. */
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "geocerca_envio_puntos")
public class GeocercaPunto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "geocerca_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private GeocercaEnvio geocerca;

    @Column(nullable = false)
    private Integer orden;

    @Column(nullable = false)
    private Double latitud;

    @Column(nullable = false)
    private Double longitud;
}
