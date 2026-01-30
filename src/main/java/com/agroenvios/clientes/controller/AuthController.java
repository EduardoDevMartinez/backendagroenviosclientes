package com.agroenvios.clientes.controller;

import com.agroenvios.clientes.dto.auth.RequestRegister;
import com.agroenvios.clientes.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@Valid @RequestBody RequestRegister request) {
        return authService.registerUser(request);
    }



}
