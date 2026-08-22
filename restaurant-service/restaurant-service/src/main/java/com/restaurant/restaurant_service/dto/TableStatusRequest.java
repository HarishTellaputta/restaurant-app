package com.restaurant.restaurant_service.dto;

import com.restaurant.restaurant_service.entity.TableStatus;
import jakarta.validation.constraints.NotNull;

public record TableStatusRequest(

        @NotNull
        TableStatus status

) {
}