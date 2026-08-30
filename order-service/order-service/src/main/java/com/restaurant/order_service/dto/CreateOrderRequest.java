package com.restaurant.order_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record CreateOrderRequest(

        Long tableId,

        Boolean tableBooking,

        @FutureOrPresent
        LocalDate bookingDate,

        @NotNull
        LocalTime bookingTime,

        @NotEmpty
        List<@Valid CreateOrderItemRequest> items

) {
}