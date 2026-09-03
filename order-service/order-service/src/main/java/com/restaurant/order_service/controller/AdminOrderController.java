package com.restaurant.order_service.controller;

import com.restaurant.order_service.dto.OrderResponse;
import com.restaurant.order_service.dto.UpdateOrderStatusRequest;
import com.restaurant.order_service.entity.OrderStatus;
import com.restaurant.order_service.entity.OrderType;
import com.restaurant.order_service.entity.PaymentStatus;
import com.restaurant.order_service.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
//@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderService orderService;


    // =====================================================
    // GET ALL ORDERS
    // =====================================================

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getAllOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) OrderType orderType,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {

        return ResponseEntity.ok(
                orderService.getAllOrders(
                        status,
                        orderType,
                        paymentStatus,
                        pageable
                )
        );
    }


    // =====================================================
    // GET ORDER BY ID
    // =====================================================

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable Long id
    ) {

        OrderResponse response =
                orderService.getOrderByIdForAdmin(id);

        return ResponseEntity.ok(response);
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
            @PathVariable Long id
    ) {

        orderService.cancelOrderInternal(id);

        return ResponseEntity.noContent().build();
    }


    // =====================================================
    // FILTER ORDERS BY STATUS
    // =====================================================

    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderResponse>> getOrdersByStatus(
            @PathVariable OrderStatus status
    ) {

        List<OrderResponse> orders =
                orderService.getOrdersByStatus(status);

        return ResponseEntity.ok(orders);
    }


    // =====================================================
    // TODAY ORDERS
    // =====================================================

    @GetMapping("/today")
    public ResponseEntity<List<OrderResponse>> getTodayOrders() {

        List<OrderResponse> orders =
                orderService.getTodayOrders();

        return ResponseEntity.ok(orders);
    }
}

