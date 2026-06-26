package com.agroenvios.clientes.primary.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PushTokenRequest {

    @NotBlank
    private String token;
}
