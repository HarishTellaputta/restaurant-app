package com.restaurant.order_service.controller;

import com.restaurant.order_service.dto.CreateOrderRequest;
import com.restaurant.order_service.dto.OrderResponse;
import com.restaurant.order_service.entity.OrderStatus;
import com.restaurant.order_service.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;


    // =====================================================
    // CREATE ORDER
    // =====================================================

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            Authentication authentication,
            @Valid @RequestBody CreateOrderRequest request
    ) {

        Long customerId =
                (Long) authentication.getPrincipal();

        OrderResponse response =
                orderService.createOrder(
                        customerId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // =====================================================
    // GET ORDER BY ID
    // =====================================================

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable Long id,
            Authentication authentication
    ) {

        Long customerId =
                (Long) authentication.getPrincipal();

        return ResponseEntity.ok(
                orderService.getOrderById(
                        id,
                        customerId
                )
        );
    }


    // =====================================================
    // MY ORDERS
    // =====================================================

    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderResponse>> getMyOrders(
            Authentication authentication
    ) {

        Long customerId =
                (Long) authentication.getPrincipal();

        return ResponseEntity.ok(
                orderService.getMyOrders(customerId)
        );
    }


    // =====================================================
    // ORDER STATUS
    // =====================================================

    @GetMapping("/{id}/status")
    public ResponseEntity<OrderStatus> getOrderStatus(
            @PathVariable Long id,
            Authentication authentication
    ) {

        Long customerId =
                (Long) authentication.getPrincipal();

        return ResponseEntity.ok(
                orderService.getOrderStatus(
                        id,
                        customerId
                )
        );
    }


    // =====================================================
    // CANCEL ORDER
    // =====================================================

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable Long id,
            Authentication authentication
    ) {

        Long customerId =
                (Long) authentication.getPrincipal();

        orderService.cancelOrder(
                id,
                customerId
        );

        return ResponseEntity.noContent().build();
    }


    // =====================================================
    // GET ORDER BY BOOKING
    // =====================================================

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<OrderResponse> getOrderByBookingId(
            @PathVariable Long bookingId,
            Authentication authentication
    ) {

        Long customerId =
                (Long) authentication.getPrincipal();

        return ResponseEntity.ok(
                orderService.getOrderByBookingId(
                        bookingId,
                        customerId
                )
        );
    }


    // =====================================================
    // INTERNAL CANCEL
    // =====================================================

    @PutMapping("/internal/{id}/cancel")
    public ResponseEntity<Void> cancelBookingOrder(
            @PathVariable Long id
    ) {

        orderService.cancelOrderInternal(id);

        return ResponseEntity.noContent().build();
    }
}