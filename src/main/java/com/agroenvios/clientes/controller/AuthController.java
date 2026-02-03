package com.agroenvios.clientes.controller;

import com.agroenvios.clientes.dto.auth.*;
import com.agroenvios.clientes.service.AuthService;
import com.agroenvios.clientes.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@Valid @RequestBody RequestRegister request) {
        return authService.registerUser(request);
    }


    @PostMapping("/login")
    public ResponseEntity<ResponseLogin> login(@Valid @RequestBody RequestLogin request) {
        return authService.login(request);
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<String> requestPasswordReset(@Valid @RequestBody RequestPasswordReset request) {
        return passwordResetService.requestPasswordReset(request.getCorreo());
    }

    @GetMapping("/password-reset/validate")
    public ResponseEntity<String> validateResetToken(@RequestParam String token) {
        return passwordResetService
                .validateResetToken(token);
    }

    @PostMapping("/password-reset")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody RequestNewPassword request) {
        return passwordResetService.resetPassword(request.getToken(), request.getPassword());
    }
}
