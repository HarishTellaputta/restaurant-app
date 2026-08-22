package com.restaurant.restaurant_service.dto;

import com.restaurant.restaurant_service.entity.TableStatus;

public record RestaurantTableResponse(

        Long id,

        Integer tableNumber,

        Integer capacity,

        TableStatus status,

        Boolean active

) {
}