package com.agro.clientes.service;

import com.agro.clientes.model.*;
import com.agro.clientes.repository.CartItemRepository;
import com.agro.clientes.repository.OrderRepository;
import com.agro.clientes.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final CustomerService customerService;
    private final CartService cartService;

    @Transactional
    public Order createOrderFromCart(Long customerId, Long cartId, String deliveryAddress, 
                                    String deliveryCity, String deliveryState, Integer deliveryPostalCode) {
        Customer customer = customerService.getCustomerById(customerId);
        List<CartItem> cartItems = cartItemRepository.findByCartId(cartId);

        if (cartItems.isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        BigDecimal totalAmount = cartItems.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .customer(customer)
                .deliveryAddress(deliveryAddress)
                .deliveryCity(deliveryCity)
                .deliveryState(deliveryState)
                .deliveryPostalCode(deliveryPostalCode)
                .status(Order.OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .build();

        Order savedOrder = orderRepository.save(order);

        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = OrderItem.builder()
                    .order(savedOrder)
                    .productId(cartItem.getProductId())
                    .productName(cartItem.getProductName())
                    .tradeShopId(cartItem.getTradeShopId())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(cartItem.getUnitPrice())
                    .subtotal(cartItem.getSubtotal())
                    .build();
            orderItemRepository.save(orderItem);
        }

        cartService.clearCart(cartId);
        return savedOrder;
    }

    public List<Order> getCustomerOrders(Long customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    public Order cancelOrder(Long orderId) {
        Order order = getOrderById(orderId);
        if (!order.getStatus().equals(Order.OrderStatus.PENDING)) {
            throw new RuntimeException("Cannot cancel order with status: " + order.getStatus());
        }
        order.setStatus(Order.OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }
}
