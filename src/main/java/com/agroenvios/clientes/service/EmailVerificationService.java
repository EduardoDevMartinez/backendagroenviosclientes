package com.agroenvios.clientes.service;

import com.agroenvios.clientes.model.User;
import com.agroenvios.clientes.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final LogsService logsService;

    public String generateVerificationToken(String username) {
        return jwtService.generateValidateUsernameToken(username);
    }

    public ResponseEntity<String> verifyEmail(String token) {
        if (!jwtService.isEmailValidationToken(token)) {
            log.warn("Intento de verificación con token inválido o expirado");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("El enlace de verificación es inválido o ha expirado");
        }

        String username = jwtService.getUsernameFromToken(token);
        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null) {
            log.warn("Intento de verificación para usuario inexistente: {}", username);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Usuario no encontrado");
        }

        if (Boolean.TRUE.equals(user.getIsActive())) {
            return ResponseEntity.status(HttpStatus.OK)
                    .body("La cuenta ya está verificada");
        }

        user.setIsEmailVerified(true);
        user.setIsActive(true);
        userRepository.save(user);

        logsService.saveLog(
                "auth",
                "verify_email",
                "Cuenta verificada - Username: " + username,
                username
        );

        log.info("Cuenta verificada exitosamente para usuario: {}", username);

        return ResponseEntity.status(HttpStatus.OK)
                .body("Cuenta verificada exitosamente");
    }
}