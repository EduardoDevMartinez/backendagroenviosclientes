package com.agroenvios.clientes.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "logs")
public class LogsModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String modulo;
    private String metodo;
    private String mensaje;
    private String usuario;
    @Builder.Default
    private LocalDateTime fecha = LocalDateTime.now();
}
