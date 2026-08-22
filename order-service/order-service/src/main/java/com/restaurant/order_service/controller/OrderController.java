package com.restaurant.order_service.controller;

import com.restaurant.order_service.dto.CreateOrderRequest;
import com.restaurant.order_service.dto.OrderResponse;
import com.restaurant.order_service.dto.UpdateOrderStatusRequest;
import com.restaurant.order_service.entity.OrderStatus;
import com.restaurant.order_service.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(
            Authentication authentication,
            @Valid @RequestBody CreateOrderRequest request
    ) {

        Long customerId =
                (Long) authentication.getPrincipal();

        return orderService.createOrder(
                customerId,
                request
        );
    }

    @GetMapping("/{id}")
    public OrderResponse getOrderById(
            @PathVariable Long id,
            Authentication authentication
    ) {

        Long customerId =
                (Long) authentication.getPrincipal();

        return orderService.getOrderById(
                id,
                customerId
        );
    }

    @GetMapping("/my-orders")
    public List<OrderResponse> getMyOrders(
            Authentication authentication
    ) {

        Long customerId =
                (Long) authentication.getPrincipal();

        return orderService.getMyOrders(customerId);
    }

    @GetMapping("/{id}/status")
    public OrderStatus getOrderStatus(
            @PathVariable Long id,
            Authentication authentication
    ) {

        Long customerId =
                (Long) authentication.getPrincipal();

        return orderService.getOrderStatus(
                id,
                customerId
        );
    }

    @PutMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelOrder(
            @PathVariable Long id,
            Authentication authentication
    ) {

        Long customerId =
                (Long) authentication.getPrincipal();

        orderService.cancelOrder(
                id,
                customerId
        );
    }

    @PutMapping("/{id}/status")
    public OrderResponse updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {

        return orderService.updateOrderStatus(
                id,
                request.status()
        );
    }
}