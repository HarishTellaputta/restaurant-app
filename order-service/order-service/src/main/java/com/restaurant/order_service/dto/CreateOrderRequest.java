package com.restaurant.order_service.dto;

import com.restaurant.order_service.entity.OrderType;
import com.restaurant.order_service.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record CreateOrderRequest(

        @NotNull
        OrderType orderType,

        @NotNull
        PaymentMethod paymentMethod,
        Long tableId,

        Boolean tableBooking,


        LocalDate bookingDate,

        LocalTime bookingTime,

        @NotEmpty
        List<@Valid CreateOrderItemRequest> items

) {
}