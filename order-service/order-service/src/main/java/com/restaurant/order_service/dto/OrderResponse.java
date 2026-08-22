package com.restaurant.order_service.dto;

import com.restaurant.order_service.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
public record OrderResponse(

        Long id,

        Long customerId,

        Long tableId,

        Long bookingId,

        BigDecimal totalAmount,

        OrderStatus status,

        LocalDateTime createdAt,

        List<OrderItemResponse> items

) {
}