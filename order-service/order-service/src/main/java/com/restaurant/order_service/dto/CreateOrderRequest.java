package com.restaurant.order_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
public record CreateOrderRequest(

        @NotNull
        Long tableId,

        Long bookingId,

        @NotEmpty
        List<@Valid CreateOrderItemRequest> items

) {
}