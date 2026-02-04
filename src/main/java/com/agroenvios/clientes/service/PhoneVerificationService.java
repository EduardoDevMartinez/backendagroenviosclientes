package com.agroenvios.clientes.service;

import com.agroenvios.clientes.model.User;
import com.agroenvios.clientes.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class PhoneVerificationService {

    private final UserRepository userRepository;
    private final LogsService logsService;

    @Value("${phone.verification.expiration-ms:300000}")
    private long expirationMs;

    private static final SecureRandom secureRandom = new SecureRandom();
    private final ConcurrentHashMap<String, long[]> otpStore = new ConcurrentHashMap<>();

    public ResponseEntity<String> requestCode(User user) {
        if (Boolean.TRUE.equals(user.getIsTelefonoVerified())) {
            return ResponseEntity.ok("El número de teléfono ya está verificado");
        }

        String otp = generateOtp();
        long expiration = System.currentTimeMillis() + expirationMs;
        otpStore.put(user.getUsername(), new long[]{Long.parseLong(otp), expiration});

        // TODO: enviar OTP por SMS al número user.getTelefono()
        logsService.saveLog("auth", "phone_request_code", "OTP solicitado para verificación de teléfono", user.getUsername());
        log.info("OTP generado para {}: {}", user.getUsername(), otp);

        return ResponseEntity.ok("Código enviado al número de teléfono registrado");
    }

    public ResponseEntity<String> verifyPhone(User user, String code) {
        if (Boolean.TRUE.equals(user.getIsTelefonoVerified())) {
            return ResponseEntity.ok("El número de teléfono ya está verificado");
        }

        long[] stored = otpStore.get(user.getUsername());

        if (stored == null || System.currentTimeMillis() > stored[1]) {
            otpStore.remove(user.getUsername());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Código inválido o expirado");
        }

        if (!String.valueOf(stored[0]).equals(code)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Código incorrecto");
        }

        otpStore.remove(user.getUsername());
        user.setIsTelefonoVerified(true);
        userRepository.save(user);

        logsService.saveLog("auth", "verify_phone", "Teléfono verificado", user.getUsername());
        log.info("Teléfono verificado para: {}", user.getUsername());

        return ResponseEntity.ok("Número de teléfono verificado exitosamente");
    }

    private String generateOtp() {
        int otp = secureRandom.nextInt(900000) + 100000;
        return String.valueOf(otp);
    }
}
