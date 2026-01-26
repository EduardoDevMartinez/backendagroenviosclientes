package com.agroenvios.clientes.controller;

import com.agroenvios.clientes.model.Order;
import com.agroenvios.clientes.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping("/create")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> request) {
        try {
            Long customerId = Long.parseLong(request.get("customerId").toString());
            Long cartId = Long.parseLong(request.get("cartId").toString());
            String deliveryAddress = (String) request.get("deliveryAddress");
            String deliveryCity = (String) request.get("deliveryCity");
            String deliveryState = (String) request.get("deliveryState");
            Integer deliveryPostalCode = Integer.parseInt(request.get("deliveryPostalCode").toString());

            Order order = orderService.createOrderFromCart(customerId, cartId, deliveryAddress, deliveryCity, deliveryState, deliveryPostalCode);
            return ResponseEntity.status(201).body(order);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<?> getCustomerOrders(@PathVariable Long customerId) {
        try {
            List<Order> orders = orderService.getCustomerOrders(customerId);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable Long orderId) {
        try {
            Order order = orderService.getOrderById(orderId);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId) {
        try {
            Order order = orderService.cancelOrder(orderId);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
