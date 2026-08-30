package com.restaurant.order_service.client;

import com.restaurant.order_service.dto.TableBookingRequest;
import com.restaurant.order_service.dto.TableBookingResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "restaurant-service",
        url = "${restaurant-service.url}"
)
public interface RestaurantServiceClient {

    @PostMapping("/api/table-bookings")
    TableBookingResponse createBooking(
            @RequestBody TableBookingRequest request
    );

    @GetMapping("/api/table-bookings/{id}")
    TableBookingResponse getBooking(
            @PathVariable("id") Long bookingId
    );
}