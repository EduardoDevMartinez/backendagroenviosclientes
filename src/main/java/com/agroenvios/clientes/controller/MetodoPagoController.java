package com.agroenvios.clientes.controller;

import com.agroenvios.clientes.model.MetodoPago;
import com.agroenvios.clientes.model.User;
import com.agroenvios.clientes.service.MetodoPagoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/metodos-pago")
@RequiredArgsConstructor
public class MetodoPagoController {

    private final MetodoPagoService metodoPagoService;

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @GetMapping
    public ResponseEntity<List<MetodoPago>> getMetodosPago() {
        return ResponseEntity.ok(metodoPagoService.getMetodosPago(getCurrentUser()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MetodoPago> getMetodoPago(@PathVariable Long id) {
        return ResponseEntity.ok(metodoPagoService.getMetodoPagoById(id, getCurrentUser()));
    }

    @PostMapping
    public ResponseEntity<MetodoPago> crear(@RequestBody MetodoPago metodoPago) {
        return ResponseEntity.status(201).body(metodoPagoService.crear(metodoPago, getCurrentUser()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MetodoPago> actualizar(@PathVariable Long id, @RequestBody MetodoPago datos) {
        return ResponseEntity.ok(metodoPagoService.actualizar(id, datos, getCurrentUser()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        metodoPagoService.eliminar(id, getCurrentUser());
        return ResponseEntity.noContent().build();
    }
}
