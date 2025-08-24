package com.shopfast.mapper;

import com.shopfast.model.dto.OrderResponse;
import com.shopfast.model.entity.Order;
import com.shopfast.model.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "items", source = "orderItems")
    OrderResponse toResponse(Order order);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    OrderResponse.OrderItemResponse toItemResponse(OrderItem orderItem);

    List<OrderResponse> toResponseList(List<Order> orders);
}
