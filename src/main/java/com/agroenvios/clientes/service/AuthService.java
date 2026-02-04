package com.agroenvios.clientes.service;

import com.agroenvios.clientes.dto.auth.RequestLogin;
import com.agroenvios.clientes.dto.auth.RequestRegister;
import com.agroenvios.clientes.dto.auth.ResponseLogin;
import com.agroenvios.clientes.events.UserRegisteredEvent;
import com.agroenvios.clientes.model.InvalidToken;
import com.agroenvios.clientes.model.User;
import com.agroenvios.clientes.repository.InvalidTokenRepository;
import com.agroenvios.clientes.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final InvalidTokenRepository invalidTokenRepository;
    private final LogsService logsService;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public ResponseEntity<ResponseLogin> login(RequestLogin request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

        if (!user.getIsEmailVerified()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseLogin.builder()
                            .message("Por favor, verifique su correo electrónico antes de iniciar sesión")
                            .build());
        }

        String token = jwtService.getToken(user);

        logsService.saveLog(
                "auth",
                "login",
                "Inicio de sesión exitoso ",
                request.getUsername()
        );


        return ResponseEntity.ok(ResponseLogin.builder()
                .token(token)
                .message("Inicio de sesión exitoso")
                .build());


    }

    public ResponseEntity<String> registerUser(RequestRegister request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Intento de registro con username existente: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("El nombre de usuario ya está registrado");
        }

        if (userRepository.existsByCorreo(request.getCorreo())) {
            log.warn("Intento de registro con correo existente: {}", request.getCorreo());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("El correo electrónico ya está registrado");
        }

        if (userRepository.existsByTelefono(request.getTelefono())) {
            log.warn("Intento de registro con teléfono existente: {}", request.getTelefono());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("El número de teléfono ya está registrado");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .nombre(request.getNombre())
                .paterno(request.getPaterno())
                .materno(request.getMaterno())
                .correo(request.getCorreo())
                .telefono(request.getTelefono())
                .build();

        userRepository.save(user);

        logsService.saveLog(
                "auth",
                "register",
                "Nuevo registro de usuario - Username: " + request.getUsername() +
                        ", Nombre: " + request.getNombre() + " " + request.getPaterno() + " " + request.getMaterno() +
                        ", Correo: " + request.getCorreo() +
                        ", Teléfono: " + request.getTelefono() ,
                request.getUsername()
        );

        eventPublisher.publishEvent(new UserRegisteredEvent(user));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Usuario registrado exitosamente");
    }

    public ResponseEntity<String> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Token no proporcionado");
        }

        String token = authHeader.substring(7);
        String username = jwtService.getUsernameFromToken(token);

        invalidTokenRepository.save(new InvalidToken(token, jwtService.getExpiration(token), username));

        logsService.saveLog("auth", "logout", "Cierre de sesión exitoso", username);
        log.info("Logout exitoso para: {}", username);

        return ResponseEntity.ok("Sesión cerrada exitosamente");
    }

    public ResponseEntity<Boolean> validateSession(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.ok(false);
        }

        String token = authHeader.substring(7);

        try {
            String username = jwtService.getUsernameFromToken(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            boolean valid = jwtService.isTokenValid(token, userDetails);
            if (valid) {
                logsService.saveLog("auth", "validate_session", "Sesión validada exitosamente", username);
            }
            return ResponseEntity.ok(valid);
        } catch (Exception e) {
            log.warn("Validación de sesión fallida: {}", e.getMessage());
            return ResponseEntity.ok(false);
        }
    }
}
