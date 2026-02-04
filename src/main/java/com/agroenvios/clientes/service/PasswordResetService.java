package com.agroenvios.clientes.service;

import com.agroenvios.clientes.model.InvalidToken;
import com.agroenvios.clientes.model.User;
import com.agroenvios.clientes.repository.InvalidTokenRepository;
import com.agroenvios.clientes.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

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

    @Value("${rate.limit.email.max-attempts:3}")
    private int emailMaxAttempts;

    @Value("${rate.limit.email.window-ms:3600000}")
    private long emailWindowMs;

    private final ConcurrentHashMap<String, long[]> emailRateLimitMap = new ConcurrentHashMap<>();

    private static final String SUCCESS_MESSAGE = "Si el correo está registrado, recibirá un enlace para recuperar su contraseña";

    public ResponseEntity<String> requestPasswordReset(String correo) {
        long[] attempts = emailRateLimitMap.compute(correo, (key, existing) -> {
            long now = System.currentTimeMillis();
            if (existing == null || now - existing[0] > emailWindowMs) {
                return new long[]{now, 1};
            }
            existing[1]++;
            return existing;
        });

        if (attempts[1] > emailMaxAttempts) {
            long remainingMs = emailWindowMs - (System.currentTimeMillis() - attempts[0]);
            long remainingSeconds = Math.max(0, remainingMs / 1000);
            log.warn("Rate limit de correo excedido para: {}", correo);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", String.valueOf(remainingSeconds))
                    .body("Demasiadas solicitudes. Intente de nuevo en " + remainingSeconds + " segundos");
        }

        userRepository.findByCorreo(correo).ifPresent(user -> {
            String token = jwtService.generatePasswordResetToken(user.getUsername());
            emailService.sendPasswordResetEmail(user.getCorreo(), user.getNombre(), token);
            logsService.saveLog("auth", "password_reset_request", "Correo de recuperación enviado", user.getUsername());
            log.info("Token de recuperación generado para: {}", user.getUsername());
        });

        // Siempre retorna el mismo mensaje para no revelar si el correo existe
        return ResponseEntity.ok(SUCCESS_MESSAGE);
    }

    public ResponseEntity<String> validateResetToken(String token) {
        if (!jwtService.isPasswordResetToken(token)) {
            log.warn("Token de password reset no válido: {}", token);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("El enlace es inválido o ha expirado");
        }
        if (invalidTokenRepository.existsByToken(token)) {
            log.warn("Token de password reset ya fue usado: {}", token);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("El enlace es inválido o ha expirado");
        }
        String username = jwtService.getUsernameFromToken(token);
        logsService.saveLog("auth", "password_reset_validate", "Token de recuperación validado", username);
        return ResponseEntity.ok("Token válido");
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