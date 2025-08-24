package com.shopfast.service;

import com.shopfast.exception.OutOfStockException;
import com.shopfast.mapper.OrderMapper;
import com.shopfast.model.dto.OrderRequest;
import com.shopfast.model.dto.OrderResponse;
import com.shopfast.model.entity.Campaign;
import com.shopfast.model.entity.Order;
import com.shopfast.model.entity.OrderItem;
import com.shopfast.model.entity.Product;
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

    private final OrderMapper orderMapper;

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        log.info("Creating order for user: {}", request.getUserId());

        String userEmail = userRepository.findEmailById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Order order = createOrderEntity(request, userEmail);
        Order savedOrder = orderRepository.save(order);

        log.info("Order created successfully: {}", savedOrder.getId());
        return orderMapper.toResponse(savedOrder);
    }

    private Order createOrderEntity(OrderRequest request, String userEmail) {
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setUserEmail(userEmail);
        order.setShippingAddress(request.getShippingAddress());
        order.setStatus(Order.OrderStatus.PENDING);

        List<Long> productIds = request.getItems().stream()
                .map(OrderRequest.OrderItemRequest::getProductId)
                .distinct()
                .toList();

        log.info("V2 FIX - Batch fetching {} unique products", productIds.size());

        List<Product> products = productRepository.findByIdIn(productIds);
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, product -> product));

        List<Campaign> activeCampaigns = campaignRepository.findActiveCampaignsByProductIds(productIds, LocalDateTime.now());
        Map<Long, Campaign> campaignMap = activeCampaigns.stream()
                .collect(Collectors.toMap(campaign -> campaign.getProduct().getId(), campaign -> campaign));

        log.info("V2 FIX - Found {} active campaigns", activeCampaigns.size());

        List<OrderItem> orderItems = processOrderItems(request.getItems(), order, productMap, campaignMap);
        BigDecimal totalAmount = calculateTotalAmount(orderItems);

        order.setOrderItems(orderItems);
        order.setTotalAmount(totalAmount);

        return order;
    }

    private List<OrderItem> processOrderItems(
            List<OrderRequest.OrderItemRequest> itemRequests,
            Order order,
            Map<Long, Product> productMap,
            Map<Long, Campaign> campaignMap) {

        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderRequest.OrderItemRequest itemRequest : itemRequests) {
            int updatedRows = productRepository.decreaseStock(itemRequest.getProductId(), itemRequest.getQuantity());

            if (updatedRows == 0) {
                Product product = productMap.get(itemRequest.getProductId());
                if (product == null) {
                    throw new RuntimeException("Product not found: " + itemRequest.getProductId());
                }
                throw new OutOfStockException("Not enough stock for product: " + product.getName());
            }

            Product product = productMap.get(itemRequest.getProductId());
            if (product == null) {
                throw new RuntimeException("Product not found: " + itemRequest.getProductId());
            }

            Campaign activeCampaign = campaignMap.get(itemRequest.getProductId());
            BigDecimal unitPrice = calculateUnitPrice(product, activeCampaign);

            OrderItem orderItem = createOrderItem(order, product, itemRequest, unitPrice);
            orderItems.add(orderItem);
        }

        return orderItems;
    }

    private BigDecimal calculateUnitPrice(Product product, Campaign activeCampaign) {
        BigDecimal unitPrice = product.getPrice();
        if (activeCampaign != null && activeCampaign.isCurrentlyActive()) {
            unitPrice = activeCampaign.getDiscountedPrice();
        }
        return unitPrice;
    }

    private OrderItem createOrderItem(Order order, Product product, OrderRequest.OrderItemRequest itemRequest, BigDecimal unitPrice) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProduct(product);
        orderItem.setQuantity(itemRequest.getQuantity());
        orderItem.setUnitPrice(unitPrice);
        orderItem.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity())));
        return orderItem;
    }

    private BigDecimal calculateTotalAmount(List<OrderItem> orderItems) {
        return orderItems.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<OrderResponse> getOrdersByUserId(Long userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return orderMapper.toResponseList(orders);
    }
}
