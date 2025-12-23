package com.agro.clientes.controller;

import com.agro.clientes.model.Customer;
import com.agro.clientes.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final CustomerService customerService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String password = request.get("password");
            String nombre = request.get("nombre");

            if (customerService.findByEmail(email).isPresent()) {
                return ResponseEntity.status(409).body("Email already exists");
            }

            Customer customer = customerService.createCustomer(email, password, nombre);
            return ResponseEntity.status(201).body(Map.of("id", customer.getId(), "email", customer.getEmail()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String password = request.get("password");

            Customer customer = customerService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Invalid credentials"));

            if (!customerService.verifyPassword(password, customer.getPassword())) {
                throw new RuntimeException("Invalid credentials");
            }

            return ResponseEntity.ok(Map.of("id", customer.getId(), "email", customer.getEmail(), "nombre", customer.getNombre()));
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Error: " + e.getMessage());
        }
    }
}
