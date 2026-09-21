package com.agroenvios.clientes.primary.model;

import jakarta.persistence.*;
import lombok.*;

@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "direcciones_entrega")
public class DireccionEntrega extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String calle;

    // Nullables a propósito: las direcciones anteriores a esta columna traen el número
    // pegado dentro de `calle`, y en zonas rurales es común que no haya número.
    @Column(name = "numero_exterior", length = 20)
    private String numeroExterior;

    @Column(name = "numero_interior", length = 20)
    private String numeroInterior;

    @Column(nullable = false)
    private String ciudad;

    @Column(nullable = false)
    private String estado;

    @Column(name = "codigoPostal", nullable = false)
    private Integer codigoPostal;

    private String colonia;

    private Double latitud;

    private Double longitud;

    @Column(nullable = false)
    @Builder.Default
    private Boolean esPrincipal = false;
}
