package com.agroenvios.clientes.service;

import com.agroenvios.clientes.dto.auth.RequestLogin;
import com.agroenvios.clientes.dto.auth.RequestRegister;
import com.agroenvios.clientes.dto.auth.ResponseLogin;
import com.agroenvios.clientes.events.UserRegisteredEvent;
import com.agroenvios.clientes.model.User;
import com.agroenvios.clientes.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final LogsService logsService;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

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





}
