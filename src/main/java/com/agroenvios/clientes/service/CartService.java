package com.agroenvios.clientes.service;

import com.agroenvios.clientes.model.Cart;
import com.agroenvios.clientes.model.CartItem;
import com.agroenvios.clientes.model.Customer;
import com.agroenvios.clientes.repository.CartRepository;
import com.agroenvios.clientes.repository.CartItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CustomerService customerService;

    public Cart getOrCreateCart(Long customerId) {
        return cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> {
                    Customer customer = customerService.getCustomerById(customerId);
                    Cart cart = Cart.builder()
                            .customer(customer)
                            .totalAmount(BigDecimal.ZERO)
                            .build();
                    return cartRepository.save(cart);
                });
    }

    public CartItem addItem(Long cartId, Long productId, String productName, Long tradeShopId, 
                           String tradeShopName, Integer quantity, BigDecimal unitPrice) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));
        
        CartItem item = CartItem.builder()
                .cart(cart)
                .productId(productId)
                .productName(productName)
                .tradeShopId(tradeShopId)
                .tradeShopName(tradeShopName)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .subtotal(unitPrice.multiply(BigDecimal.valueOf(quantity)))
                .build();
        
        cartItemRepository.save(item);
        updateCartTotal(cartId);
        return item;
    }

    public void removeItem(Long itemId) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));
        cartItemRepository.delete(item);
        updateCartTotal(item.getCart().getId());
    }

    public void clearCart(Long cartId) {
        cartItemRepository.deleteByCartId(cartId);
        updateCartTotal(cartId);
    }

    private void updateCartTotal(Long cartId) {
        Cart cart = cartRepository.findById(cartId).orElseThrow();
        List<CartItem> items = cartItemRepository.findByCartId(cartId);
        BigDecimal total = items.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        cart.setTotalAmount(total);
        cartRepository.save(cart);
    }

    public Cart getCart(Long cartId) {
        return cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));
    }
}
