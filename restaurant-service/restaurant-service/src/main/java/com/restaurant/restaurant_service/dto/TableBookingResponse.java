package com.restaurant.restaurant_service.dto;

import com.restaurant.restaurant_service.entity.BookingStatus;

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

        BookingStatus status,

        LocalDateTime createdAt

) {
}