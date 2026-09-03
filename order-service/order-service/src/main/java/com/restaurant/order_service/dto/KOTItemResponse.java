package com.restaurant.order_service.dto;

public record KOTItemResponse(

        Long id,

        String foodItemName,

        Integer quantity
) {
}