package com.restaurant.order_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateOrderRequest(

        Long tableId,

        Long bookingId,

        @NotEmpty
        List<@Valid CreateOrderItemRequest> items

) {
}