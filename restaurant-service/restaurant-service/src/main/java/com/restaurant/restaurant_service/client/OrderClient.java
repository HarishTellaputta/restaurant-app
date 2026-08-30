package com.restaurant.restaurant_service.client;

import com.restaurant.restaurant_service.dto.OrderResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "order-service",
        url = "${services.order-service.url}"
)
public interface OrderClient {

    @GetMapping("/api/orders/booking/{bookingId}")
    OrderResponse getOrderByBookingId(
            @PathVariable("bookingId") Long bookingId,
            @RequestParam("customerId") Long customerId,
            @RequestParam("tableId") Long tableId
    );

    @PutMapping("/api/orders/internal/{id}/cancel")
    void cancelOrder(
            @PathVariable("id") Long orderId
    );
}