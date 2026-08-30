package com.restaurant.order_service.dto;

public record KOTItemResponse(

        Long id,

        Long foodItemId,

        Integer quantity
) {
}