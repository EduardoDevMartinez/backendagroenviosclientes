package com.agroenvios.clientes.controller;

import com.agroenvios.clientes.model.Cart;
import com.agroenvios.clientes.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @GetMapping("/{customerId}")
    public ResponseEntity<?> getCart(@PathVariable Long customerId) {
        try {
            Cart cart = cartService.getOrCreateCart(customerId);
            return ResponseEntity.ok(cart);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> addItem(@RequestBody Map<String, Object> request) {
        try {
            Long cartId = Long.parseLong(request.get("cartId").toString());
            Long productId = Long.parseLong(request.get("productId").toString());
            String productName = (String) request.get("productName");
            Long tradeShopId = Long.parseLong(request.get("tradeShopId").toString());
            String tradeShopName = (String) request.get("tradeShopName");
            Integer quantity = Integer.parseInt(request.get("quantity").toString());
            BigDecimal unitPrice = new BigDecimal(request.get("unitPrice").toString());

            var item = cartService.addItem(cartId, productId, productName, tradeShopId, tradeShopName, quantity, unitPrice);
            return ResponseEntity.ok(item);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/remove/{itemId}")
    public ResponseEntity<?> removeItem(@PathVariable Long itemId) {
        try {
            cartService.removeItem(itemId);
            return ResponseEntity.ok("Item removed");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{cartId}/clear")
    public ResponseEntity<?> clearCart(@PathVariable Long cartId) {
        try {
            cartService.clearCart(cartId);
            return ResponseEntity.ok("Cart cleared");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
