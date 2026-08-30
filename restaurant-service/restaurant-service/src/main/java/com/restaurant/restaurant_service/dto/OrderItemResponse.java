package com.restaurant.restaurant_service.dto;

import java.math.BigDecimal;

public record OrderItemResponse(

        Long id,

        Long foodItemId,

        Integer quantity,

        BigDecimal price,

        BigDecimal subtotal

) {
}