package com.agroenvios.clientes.service;

import com.agroenvios.clientes.model.InvalidToken;
import com.agroenvios.clientes.model.User;
import com.agroenvios.clientes.repository.InvalidTokenRepository;
import com.agroenvios.clientes.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final InvalidTokenRepository invalidTokenRepository;
    private final LogsService logsService;

    private static final String SUCCESS_MESSAGE = "Si el correo está registrado, recibirá un enlace para recuperar su contraseña";

    public ResponseEntity<String> requestPasswordReset(String correo) {
        userRepository.findByCorreo(correo).ifPresent(user -> {
            String token = jwtService.generatePasswordResetToken(user.getUsername());
            emailService.sendPasswordResetEmail(user.getCorreo(), user.getNombre(), token);
            log.info("Token de recuperación generado para: {}", user.getUsername());
        });

        // Siempre retorna el mismo mensaje para no revelar si el correo existe
        return ResponseEntity.ok(SUCCESS_MESSAGE);
    }

    public ResponseEntity<String> resetPassword(String token, String newPassword) {
        if (!jwtService.isPasswordResetToken(token) || invalidTokenRepository.existsByToken(token)) {
            log.warn("Intento de reset con token inválido, expirado o ya usado");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("El enlace es inválido o ha expirado");
        }

        String username = jwtService.getUsernameFromToken(token);
        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null) {
            log.warn("Intento de reset para usuario inexistente: {}", username);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("El enlace es inválido o ha expirado");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Invalidar el token para que no se reutilice
        invalidTokenRepository.save(new InvalidToken(token, jwtService.getExpiration(token), username));

        logsService.saveLog("auth", "password_reset", "Contraseña recuperada exitosamente", username);
        log.info("Contraseña recuperada para usuario: {}", username);

        return ResponseEntity.ok("Contraseña actualizada exitosamente");
    }
}