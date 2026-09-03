package com.restaurant.order_service.dto;


import java.math.BigDecimal;

public record CartItemResponse(

        Long id,

        Long foodItemId,

        String foodItemName,

        String imageUrl,

        Integer quantity,

        BigDecimal price,

        BigDecimal subtotal

) {
}