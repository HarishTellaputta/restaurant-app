package com.restaurant.menu_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FoodItemRequest(

        @NotBlank
        String name,

        String description,

        @NotNull
        @Positive
        Double price,

        String imageUrl,

        Boolean available,

        @NotNull
        Long categoryId

) {
}