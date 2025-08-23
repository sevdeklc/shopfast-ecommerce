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
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;

    private final UserRepository userRepository;

    private final ProductRepository productRepository;

    private final CampaignRepository campaignRepository;

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

        // V2 FIX: Collect all product IDs to eliminate N+1 Query Problem
        List<Long> productIds = request.getItems().stream()
                .map(OrderRequest.OrderItemRequest::getProductId)
                .distinct()
                .toList();

        log.info("V2 FIX - Batch fetching {} unique products", productIds.size());

        // V2 FIX: Batch fetch all products in single query
        List<Product> products = productRepository.findByIdIn(productIds);
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, product -> product));

        // V2 FIX: Batch fetch all campaigns in single query
        List<Campaign> activeCampaigns = campaignRepository.findActiveCampaignsByProductIds(productIds, LocalDateTime.now());
        Map<Long, Campaign> campaignMap = activeCampaigns.stream()
                .collect(Collectors.toMap(campaign -> campaign.getProduct().getId(), campaign -> campaign));

        log.info("V2 FIX - Found {} active campaigns for products", activeCampaigns.size());

        // Process each item with pre-fetched data
        for (OrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            // V2 FIX: Atomic stock decrease (already optimized)
            int updatedRows = productRepository.decreaseStock(itemRequest.getProductId(), itemRequest.getQuantity());

            if (updatedRows == 0) {
                Product product = productMap.get(itemRequest.getProductId());
                if (product == null) {
                    throw new RuntimeException("Product not found: " + itemRequest.getProductId());
                }
                throw new OutOfStockException("Not enough stock for product: " + product.getName());
            }

            // V2 FIX: Get product from pre-fetched map instead of database query
            Product product = productMap.get(itemRequest.getProductId());
            if (product == null) {
                throw new RuntimeException("Product not found: " + itemRequest.getProductId());
            }

            // V2 FIX: Get campaign from pre-fetched map instead of database query
            Campaign activeCampaign = campaignMap.get(itemRequest.getProductId());

            BigDecimal unitPrice = product.getPrice();
            if (activeCampaign != null && activeCampaign.isCurrentlyActive()) {
                unitPrice = activeCampaign.getDiscountedPrice();
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setUnitPrice(unitPrice);
            orderItem.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity())));

            orderItems.add(orderItem);
            totalAmount = totalAmount.add(orderItem.getTotalPrice());

            // Problem 5: Slow operations (simulation) - keeping for now
            simulateSlowProcess();
        }

        order.setOrderItems(orderItems);
        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);
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
