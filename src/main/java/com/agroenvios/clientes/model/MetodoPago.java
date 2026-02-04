package com.agroenvios.clientes.model;

import jakarta.persistence.*;
import lombok.*;

@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "metodos_pago")
public class MetodoPago extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoPago tipo;

    @Column(nullable = false)
    private String titular;

    @Column(nullable = false, length = 4)
    private String ultimosDigitos;

    private String fechaExpiracion;

    @Column(nullable = false)
    @Builder.Default
    private Boolean esPrincipal = false;

    public enum TipoPago {
        TARJETA_CREDITO, TARJETA_DEBITO, TRANSFERENCIA
    }
}
