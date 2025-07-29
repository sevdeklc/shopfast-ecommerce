package com.shopfast.service;

import com.shopfast.exception.OutOfStockException;
import com.shopfast.model.dto.OrderRequest;
import com.shopfast.model.dto.OrderResponse;
import com.shopfast.model.entity.Campaign;
import com.shopfast.model.entity.Order;
import com.shopfast.model.entity.OrderItem;
import com.shopfast.model.entity.Product;
import com.shopfast.model.entity.User;
import com.shopfast.repository.CampaignRepository;
import com.shopfast.repository.OrderRepository;
import com.shopfast.repository.ProductRepository;
import com.shopfast.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;

    private final UserRepository userRepository;

    private final ProductRepository productRepository;

    private final CampaignRepository campaignRepository;

    /**
     * VERSION 1 (BAD) - This version will suffer from performance issues
     * Problems:
     * 1. N+1 Query Problem
     * 2. Race Condition (Stock control)
     * 3. Long running transaction
     * 4. No caching
     * 5. Synchronous processing
     */

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        log.info("Creating order for user: {}", request.getUserId());

        // Problem 1: We are fetching the user from the database every time
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Order order = new Order();
        order.setUser(user);
        order.setShippingAddress(request.getShippingAddress());
        order.setStatus(Order.OrderStatus.PENDING);

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        // Problem 2: A separate database query is executed for each item (N+1 Problem)
        for (OrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            // Problem 3: Race condition - Stock control is not atomic
            if (product.getStockQuantity() < itemRequest.getQuantity()) {
                throw new OutOfStockException("Not enough stock for product: " + product.getName());
            }

            // Campaign check (querying the database every time)
            Campaign activeCampaign = campaignRepository
                    .findActiveCampaignByProduct(product, LocalDateTime.now())
                    .orElse(null);

            BigDecimal unitPrice = product.getPrice();
            if (activeCampaign != null && activeCampaign.isCurrentlyActive()) {
                unitPrice = activeCampaign.getDiscountedPrice();
            }

            // Problem 4: Stock update is vulnerable to race conditions
            product.setStockQuantity(product.getStockQuantity() - itemRequest.getQuantity());
            productRepository.save(product); // Separate save operation for each item

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setUnitPrice(unitPrice);
            orderItem.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity())));

            orderItems.add(orderItem);
            totalAmount = totalAmount.add(orderItem.getTotalPrice());

            // Problem 5: Slow operations (simulation)
            simulateSlowProcess();
        }

        order.setOrderItems(orderItems);
        order.setTotalAmount(totalAmount);

        // Problem 6: Large transaction – long lock duration
        Order savedOrder = orderRepository.save(order);

        // Problem 7: Synchronous notification (slow)
        sendOrderNotification(savedOrder);

        log.info("Order created successfully: {}", savedOrder.getId());
        return convertToResponse(savedOrder);
    }

    // Slow operation simulation
    private void simulateSlowProcess() {
        try {
            // Database query and external service call simulation
            Thread.sleep(100); // 100ms delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Sending synchronous notifications (slow)
    private void sendOrderNotification(Order order) {
        try {
            // Email/SMS sending simulation
            Thread.sleep(200); // 200ms delay
            log.info("Notification sent for order: {}", order.getId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private OrderResponse convertToResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setUserId(order.getUser().getId());
        response.setUserEmail(order.getUser().getEmail());
        response.setTotalAmount(order.getTotalAmount());
        response.setStatus(order.getStatus());
        response.setShippingAddress(order.getShippingAddress());
        response.setCreatedAt(order.getCreatedAt());

        List<OrderResponse.OrderItemResponse> itemResponses = new ArrayList<>();
        for (OrderItem item : order.getOrderItems()) {
            OrderResponse.OrderItemResponse itemResponse = new OrderResponse.OrderItemResponse();
            itemResponse.setId(item.getId());
            itemResponse.setProductId(item.getProduct().getId());
            itemResponse.setProductName(item.getProduct().getName());
            itemResponse.setQuantity(item.getQuantity());
            itemResponse.setUnitPrice(item.getUnitPrice());
            itemResponse.setTotalPrice(item.getTotalPrice());
            itemResponses.add(itemResponse);
        }
        response.setItems(itemResponses);

        return response;
    }

    public List<OrderResponse> getOrdersByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Order> orders = orderRepository.findByUserOrderByCreatedAtDesc(user);
        return orders.stream().map(this::convertToResponse).toList();
    }
}
