package com.restaurant.order_service.service;

import com.restaurant.order_service.client.MenuServiceClient;
import com.restaurant.order_service.dto.*;
import com.restaurant.order_service.entity.Order;
import com.restaurant.order_service.entity.OrderItem;
import com.restaurant.order_service.entity.OrderStatus;
import com.restaurant.order_service.repository.OrderItemRepository;
import com.restaurant.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.http.HttpStatus.ACCEPTED;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MenuServiceClient menuServiceClient;

    @Transactional
    public OrderResponse createOrder(
            Long customerId,
            CreateOrderRequest request
    ) {

        Order order = Order.builder()
                .customerId(customerId)
                .tableId(request.tableId())
                .totalAmount(BigDecimal.ZERO)
                .status(OrderStatus.PLACED)
                .build();

        order = orderRepository.save(order);

        BigDecimal total = BigDecimal.ZERO;

        for (CreateOrderItemRequest itemRequest : request.items()) {

            FoodItemResponse foodItem =
                    menuServiceClient.getFoodItem(
                            itemRequest.foodItemId()
                    );

            if (foodItem == null) {
                throw new RuntimeException(
                        "Food item not found with id: "
                                + itemRequest.foodItemId()
                );
            }

            if (!Boolean.TRUE.equals(foodItem.available())) {
                throw new RuntimeException(
                        "Food item is not available: "
                                + foodItem.name()
                );
            }

            BigDecimal price =
                    BigDecimal.valueOf(foodItem.price());

            BigDecimal subtotal =
                    price.multiply(
                            BigDecimal.valueOf(
                                    itemRequest.quantity()
                            )
                    );

            OrderItem orderItem = OrderItem.builder()
                    .orderId(order.getId())
                    .foodItemId(foodItem.id())
                    .quantity(itemRequest.quantity())
                    .price(price)
                    .subtotal(subtotal)
                    .build();

            orderItemRepository.save(orderItem);

            total = total.add(subtotal);
        }

        order.setTotalAmount(total);

        order = orderRepository.save(order);

        return buildOrderResponse(order);
    }

    public OrderResponse getOrderById(
            Long orderId,
            Long customerId
    ) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Order not found with id: " + orderId
                        ));

        if (!order.getCustomerId().equals(customerId)) {
            throw new RuntimeException(
                    "You are not authorized to view this order"
            );
        }

        return buildOrderResponse(order);
    }

    public List<OrderResponse> getMyOrders(Long customerId) {

        return orderRepository
                .findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(this::buildOrderResponse)
                .toList();
    }

    public OrderStatus getOrderStatus(
            Long orderId,
            Long customerId
    ) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Order not found with id: " + orderId
                        ));

        if (!order.getCustomerId().equals(customerId)) {
            throw new RuntimeException(
                    "You are not authorized to view this order"
            );
        }

        return order.getStatus();
    }private OrderResponse buildOrderResponse(Order order) {

        List<OrderItemResponse> items =
                orderItemRepository.findByOrderId(order.getId())
                        .stream()
                        .map(item -> new OrderItemResponse(
                                item.getId(),
                                item.getFoodItemId(),
                                item.getQuantity(),
                                item.getPrice(),
                                item.getSubtotal()
                        ))
                        .toList();

        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getTableId(),
                order.getBookingId(),       // ✅ ADD THIS
                order.getTotalAmount(),
                order.getStatus(),
                order.getCreatedAt(),
                items
        );
    }

    @Transactional
    public void cancelOrder(
            Long orderId,
            Long customerId
    ) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Order not found with id: " + orderId
                        ));

        // Make sure this order belongs to the logged-in customer
        if (!order.getCustomerId().equals(customerId)) {
            throw new RuntimeException(
                    "You are not authorized to cancel this order"
            );
        }

        // Customer can cancel only newly placed orders
        if (order.getStatus() != OrderStatus.PLACED) {
            throw new RuntimeException(
                    "Order cannot be cancelled in current status: "
                            + order.getStatus()
            );
        }

        order.setStatus(OrderStatus.CANCELLED);

        orderRepository.save(order);
    }
    @Transactional
    public OrderResponse updateOrderStatus(
            Long orderId,
            OrderStatus newStatus
    ) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Order not found with id: " + orderId
                        ));

        OrderStatus currentStatus = order.getStatus();

        if (currentStatus == OrderStatus.CANCELLED) {
            throw new RuntimeException(
                    "Cancelled order cannot be updated"
            );
        }

        if (currentStatus == OrderStatus.COMPLETED) {
            throw new RuntimeException(
                    "Completed order cannot be updated"
            );
        }

        if (!isValidTransition(currentStatus, newStatus)) {
            throw new RuntimeException(
                    "Invalid order status transition: "
                            + currentStatus
                            + " -> "
                            + newStatus
            );
        }

        order.setStatus(newStatus);

        order = orderRepository.save(order);

        return buildOrderResponse(order);
    }

    private boolean isValidTransition(
            OrderStatus currentStatus,
            OrderStatus newStatus
    ) {

        switch (currentStatus) {

            case PLACED:
                return newStatus == OrderStatus.ACCEPTED
                        || newStatus == OrderStatus.CANCELLED;

            case ACCEPTED:
                return newStatus == OrderStatus.PREPARING;

            case PREPARING:
                return newStatus == OrderStatus.READY;

            case READY:
                return newStatus == OrderStatus.COMPLETED;

            case COMPLETED:
                return false;

            case CANCELLED:
                return false;

            default:
                return false;
        }
    }
}