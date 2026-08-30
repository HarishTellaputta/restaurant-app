package com.restaurant.order_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalTime;

public record TableBookingRequest(

        @NotNull
        Long customerId,

        @NotNull
        Long tableId,

        @NotNull
        LocalDate bookingDate,

        @NotNull
        LocalTime bookingTime,

        @NotNull
        @Positive
        Integer guests

) {
}