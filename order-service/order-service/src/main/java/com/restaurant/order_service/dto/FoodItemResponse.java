package com.restaurant.order_service.dto;

public record FoodItemResponse(

        Long id,

        String name,

        String description,

        Double price,

        String imageUrl,

        Boolean available

) {
}