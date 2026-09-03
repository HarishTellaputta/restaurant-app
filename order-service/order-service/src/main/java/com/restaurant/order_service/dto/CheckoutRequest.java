package com.restaurant.order_service.dto;

import com.restaurant.order_service.entity.OrderType;
import com.restaurant.order_service.entity.PaymentMethod;

import java.time.LocalDate;
import java.time.LocalTime;

public record CheckoutRequest(

        OrderType orderType,

        PaymentMethod paymentMethod,

        // DINE_IN
        Long tableId,

        Boolean tableBooking,

        LocalDate bookingDate,

        LocalTime bookingTime,

        // DELIVERY
        Double distanceKm,

        Boolean peakTime,

        String weather,

        // Optional coupon
        String couponCode

) {
}