package com.example.ordermanagement.mapper;

import com.example.ordermanagement.dto.response.OrderItemResponse;
import com.example.ordermanagement.dto.response.OrderResponse;
import com.example.ordermanagement.entity.Order;
import com.example.ordermanagement.entity.OrderItem;

public final class OrderMapper {

    private OrderMapper() {
    }

    public static OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .customer(CustomerMapper.toResponse(order.getCustomer()))
                .items(order.getOrderItems().stream().map(OrderMapper::toItemResponse).toList())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .build();
    }

    private static OrderItemResponse toItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getSubtotal())
                .build();
    }
}
