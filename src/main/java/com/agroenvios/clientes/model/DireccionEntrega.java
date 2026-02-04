package com.agroenvios.clientes.model;

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

    @Column(nullable = false)
    private String ciudad;

    @Column(nullable = false)
    private String estado;

    @Column(nullable = false)
    private Integer codigoPostal;

    private String colonia;

    @Column(nullable = false)
    @Builder.Default
    private Boolean esPrincipal = false;
}
