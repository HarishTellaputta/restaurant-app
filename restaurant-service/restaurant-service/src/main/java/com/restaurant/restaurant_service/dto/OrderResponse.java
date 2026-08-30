package com.restaurant.restaurant_service.dto;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
public record OrderResponse(

        Long id,

        Long customerId,

        Long tableId,

        Long bookingId,

        BigDecimal totalAmount,

        String status,

        LocalDateTime createdAt,

        List<OrderItemResponse> items

) {
}