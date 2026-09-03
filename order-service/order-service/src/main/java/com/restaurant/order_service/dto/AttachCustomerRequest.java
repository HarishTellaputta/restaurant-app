package com.restaurant.order_service.dto;

import jakarta.validation.constraints.NotNull;

public record AttachCustomerRequest(

        @NotNull
        Long customerId

) {
}