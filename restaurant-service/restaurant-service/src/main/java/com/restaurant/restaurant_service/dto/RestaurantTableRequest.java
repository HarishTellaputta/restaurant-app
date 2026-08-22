package com.restaurant.restaurant_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RestaurantTableRequest(

        @NotNull
        @Positive
        Integer tableNumber,

        @NotNull
        @Positive
        Integer capacity,

        Boolean active

) {
}