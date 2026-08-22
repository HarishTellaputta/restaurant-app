package com.restaurant.order_service.client;

import com.restaurant.order_service.dto.TableBookingResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "restaurant-service",
        url = "${restaurant-service.url}"
)
public interface RestaurantServiceClient {

    @GetMapping("/api/table-bookings/{id}")
    TableBookingResponse getBooking(
            @PathVariable("id") Long bookingId
    );
}