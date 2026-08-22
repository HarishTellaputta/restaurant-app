package com.restaurant.order_service.dto;

import com.restaurant.order_service.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(

        @NotNull
        OrderStatus status

) {
}