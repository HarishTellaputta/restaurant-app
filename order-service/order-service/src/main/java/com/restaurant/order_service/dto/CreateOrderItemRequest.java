package com.restaurant.order_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateOrderItemRequest(

        @NotNull
        Long foodItemId,

        @NotNull
        @Min(1)
        Integer quantity

) {
}