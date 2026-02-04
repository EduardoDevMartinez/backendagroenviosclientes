package com.agroenvios.clientes.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseUser {
    private Long id;
    private String username;
    private String nombre;
    private String paterno;
    private String materno;
    private String correo;
    private String telefono;
    private String foto;
    private Boolean isEmailVerified;
    private Boolean isTelefonoVerified;
    private Boolean isActive;
}
