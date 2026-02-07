package com.agroenvios.clientes.controller;

import com.agroenvios.clientes.dto.direccion.RequestDireccion;
import com.agroenvios.clientes.model.DireccionEntrega;
import com.agroenvios.clientes.model.User;
import com.agroenvios.clientes.service.DireccionEntregaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/direcciones")
@RequiredArgsConstructor
public class DireccionEntregaController {

    private final DireccionEntregaService direccionService;

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @GetMapping
    public ResponseEntity<List<DireccionEntrega>> getDirecciones() {
        return ResponseEntity.ok(direccionService.getDirecciones(getCurrentUser()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DireccionEntrega> getDireccion(@PathVariable Long id) {
        return ResponseEntity.ok(direccionService.getDireccionById(id, getCurrentUser()));
    }

    @PostMapping
    public ResponseEntity<DireccionEntrega> crear(@Valid @RequestBody RequestDireccion request) {
        return ResponseEntity.status(201).body(direccionService.crear(request, getCurrentUser()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DireccionEntrega> actualizar(@PathVariable Long id, @RequestBody DireccionEntrega datos) {
        return ResponseEntity.ok(direccionService.actualizar(id, datos, getCurrentUser()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        direccionService.eliminar(id, getCurrentUser());
        return ResponseEntity.noContent().build();
    }
}
