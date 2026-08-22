package com.restaurant.menu_service.dto;

public record FoodItemResponse(

        Long id,

        String name,

        String description,

        Double price,

        String imageUrl,

        Boolean available,

        Long categoryId,

        String categoryName

) {
}