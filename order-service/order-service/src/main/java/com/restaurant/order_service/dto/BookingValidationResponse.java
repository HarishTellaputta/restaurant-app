package com.restaurant.order_service.dto;

public record BookingValidationResponse(

        Long id,

        Long customerId,

        Long tableId,

        String status

) {
}