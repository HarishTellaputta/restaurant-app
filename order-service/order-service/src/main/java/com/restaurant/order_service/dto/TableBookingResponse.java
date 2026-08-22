package com.restaurant.order_service.dto;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record TableBookingResponse(

        Long id,

        Long customerId,

        Long tableId,

        LocalDate bookingDate,

        LocalTime bookingTime,

        Integer guests,

        String status,

        LocalDateTime createdAt

) {
}