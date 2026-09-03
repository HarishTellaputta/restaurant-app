package com.restaurant.order_service.controller;

import com.restaurant.order_service.dto.*;
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
    // UPDATE ORDER STATUS
    // =====================================================

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {

        OrderResponse response =
                orderService.updateOrderStatus(
                        id,
                        request.status()
                );

        return ResponseEntity.ok(response);
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

    @PostMapping("/dine-in/start")
    public ResponseEntity<OrderResponse> startDineInOrder(
            @Valid @RequestBody StartDineInOrderRequest request
    ) {

        OrderResponse response =
                orderService.startDineInOrder(
                        request.customerId()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // =====================================================
// ATTACH CUSTOMER TO DINE-IN ORDER
// =====================================================

    @PutMapping("/{orderId}/customer")
    public ResponseEntity<OrderResponse> attachCustomer(
            @PathVariable Long orderId,
            @Valid @RequestBody AttachCustomerRequest request
    ) {

        return ResponseEntity.ok(
                orderService.attachCustomer(
                        orderId,
                        request.customerId()
                )
        );
    }

    @PostMapping("/{orderId}/dine-in/items")
    public ResponseEntity<OrderResponse> addDineInItems(
            @PathVariable Long orderId,
            @Valid @RequestBody AddDineInItemsRequest request
    ) {

        return ResponseEntity.ok(
                orderService.addDineInItems(
                        orderId,
                        request
                )
        );
    }

    @PostMapping("/{orderId}/dine-in/send-to-kitchen")
    public ResponseEntity<Long> sendItemsToKitchen(
            @PathVariable Long orderId,
            @RequestParam(required = false) String generatedBy
    ) {

        Long kotId =
                orderService.sendItemsToKitchen(
                        orderId,
                        generatedBy
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(kotId);
    }
    @PutMapping("/{orderId}/dine-in/final-submit")
    public ResponseEntity<OrderResponse> finalSubmitDineInOrder(
            @PathVariable Long orderId
    ) {

        return ResponseEntity.ok(
                orderService.finalSubmitDineInOrder(orderId)
        );
    }

    @PostMapping("/from-cart")
    public ResponseEntity<OrderResponse> createOrderFromCart(
            Authentication authentication,
            @Valid @RequestBody CheckoutRequest request
    ) {

        Long customerId =
                (Long) authentication.getPrincipal();

        OrderResponse response =
                orderService.createOrderFromCart(
                        customerId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

}

