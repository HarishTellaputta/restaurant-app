package com.restaurant.order_service.service;

import com.restaurant.order_service.client.MenuServiceClient;
import com.restaurant.order_service.client.RestaurantServiceClient;
import com.restaurant.order_service.dto.*;
import com.restaurant.order_service.entity.*;
import com.restaurant.order_service.repository.KOTItemRepository;
import com.restaurant.order_service.repository.KOTRepository;
import com.restaurant.order_service.repository.OrderItemRepository;
import com.restaurant.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MenuServiceClient menuServiceClient;
    private final RestaurantServiceClient restaurantServiceClient;
    private final DeliveryChargeService deliveryChargeService;
    private final KOTRepository kotRepository;
    private final KOTItemRepository kotItemRepository;


    // =====================================================
    // CREATE ORDER
    // =====================================================

    @Transactional
    public OrderResponse createOrder(
            Long customerId,
            CreateOrderRequest request
    ) {

        log.info(
                "Creating order | customerId={} | orderType={} | tableBooking={}",
                customerId,
                request.orderType(),
                request.tableBooking()
        );

        boolean tableBooking =
                Boolean.TRUE.equals(request.tableBooking());

        Long bookingId = null;

        // =====================================================
        // TABLE BOOKING
        // =====================================================

        if (tableBooking) {

            if (request.orderType() != OrderType.DINE_IN) {

                throw new RuntimeException(
                        "Table booking is allowed only for DINE_IN orders"
                );
            }

            if (request.bookingDate() == null) {

                throw new RuntimeException(
                        "Booking date is required when table booking is selected"
                );
            }

            if (request.bookingTime() == null) {

                throw new RuntimeException(
                        "Booking time is required when table booking is selected"
                );
            }
            if (request.tableId() == null) {

                throw new RuntimeException(
                        "tableId is required when table booking is selected"
                );
            }
            TableBookingRequest tableBookingRequest = new TableBookingRequest(
                    customerId,
                    request.tableId(),
                    request.bookingDate(),
                    request.bookingTime(),
                    4
            );
            // -------------------------------------------------
            // CALL RESTAURANT SERVICE
            // -------------------------------------------------

            TableBookingResponse tableBookingResponse= restaurantServiceClient.createBooking(tableBookingRequest);
        }


        // =====================================================
        // CREATE ORDER
        // =====================================================

        Order order =
                Order.builder()
                        .customerId(customerId)
                        .bookingId(bookingId)
                        .tableBooking(request.tableBooking())
                        .orderType(request.orderType())
                        .paymentMethod(request.paymentMethod())
                        .paymentStatus(
                                request.paymentMethod() == PaymentMethod.ONLINE
                                        ? PaymentStatus.PENDING
                                        : PaymentStatus.PENDING
                        )
                        .totalAmount(BigDecimal.ZERO)
                        .status(OrderStatus.PLACED)
                        .build();

        order = orderRepository.save(order);


        // =====================================================
        // CREATE ORDER ITEMS
        // =====================================================

        for (CreateOrderItemRequest itemRequest : request.items()) {

            FoodItemResponse foodItem =
                    menuServiceClient.getFoodItem(
                            itemRequest.foodItemId()
                    );

            if (foodItem == null) {
                throw new RuntimeException(
                        "Food item not found: " + itemRequest.foodItemId()
                );
            }

            if (!foodItem.available()) {
                throw new RuntimeException(
                        "Food item is currently unavailable: "
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

            OrderItem item =
                    OrderItem.builder()
                            .orderId(order.getId())
                            .foodItemId(foodItem.id())
                            .quantity(itemRequest.quantity())
                            .price(price)
                            .subtotal(subtotal)
                            .kotId(null)
                            .build();

            orderItemRepository.save(item);
        }


        // =====================================================
        // RECALCULATE TOTAL
        // =====================================================

        recalculateOrderTotal(order);

        return buildOrderResponse(order);
    }

    // =====================================================
    // GET ORDER BY ID
    // =====================================================

    public OrderResponse getOrderById(
            Long orderId,
            Long customerId
    ) {

        log.info(
                "Fetching order by id | orderId={} | customerId={}",
                orderId,
                customerId
        );


        Order order =
                orderRepository.findById(orderId)
                        .orElseThrow(() -> {

                            log.warn(
                                    "Order not found | orderId={}",
                                    orderId
                            );

                            return new RuntimeException(
                                    "Order not found with id: "
                                            + orderId
                            );
                        });


        if (!order.getCustomerId().equals(customerId)) {

            log.warn(
                    "Unauthorized order access | orderId={} | orderCustomerId={} | requestedCustomerId={}",
                    orderId,
                    order.getCustomerId(),
                    customerId
            );

            throw new RuntimeException(
                    "You are not authorized to view this order"
            );
        }


        log.info(
                "Order fetched successfully | orderId={} | status={}",
                orderId,
                order.getStatus()
        );


        return buildOrderResponse(order);
    }


    // =====================================================
    // GET MY ORDERS
    // =====================================================

    public List<OrderResponse> getMyOrders(
            Long customerId
    ) {

        log.info(
                "Fetching customer orders | customerId={}",
                customerId
        );


        List<OrderResponse> orders =
                orderRepository
                        .findByCustomerIdOrderByCreatedAtDesc(
                                customerId
                        )
                        .stream()
                        .map(this::buildOrderResponse)
                        .toList();


        log.info(
                "Customer orders fetched successfully | customerId={} | orderCount={}",
                customerId,
                orders.size()
        );


        return orders;
    }


    // =====================================================
    // GET ORDER STATUS
    // =====================================================

    public OrderStatus getOrderStatus(
            Long orderId,
            Long customerId
    ) {

        log.info(
                "Fetching order status | orderId={} | customerId={}",
                orderId,
                customerId
        );


        Order order =
                orderRepository.findById(orderId)
                        .orElseThrow(() -> {

                            log.warn(
                                    "Order not found while fetching status | orderId={}",
                                    orderId
                            );

                            return new RuntimeException(
                                    "Order not found with id: "
                                            + orderId
                            );
                        });


        if (!order.getCustomerId().equals(customerId)) {

            log.warn(
                    "Unauthorized order status access | orderId={} | customerId={}",
                    orderId,
                    customerId
            );

            throw new RuntimeException(
                    "You are not authorized to view this order"
            );
        }


        log.info(
                "Order status fetched | orderId={} | status={}",
                orderId,
                order.getStatus()
        );


        return order.getStatus();
    }


    // =====================================================
    // GET ORDER BY BOOKING ID
    // =====================================================

    public OrderResponse getOrderByBookingId(
            Long bookingId,
            Long customerId
    ) {

        log.info(
                "Fetching order by booking | bookingId={} | customerId={}",
                bookingId,
                customerId
        );


        Order order =
                orderRepository
                        .findFirstByCustomerIdAndBookingId(
                                customerId,
                                bookingId
                        )
                        .orElseThrow(() -> {

                            log.warn(
                                    "Order not found | bookingId={} | customerId={}",
                                    bookingId,
                                    customerId
                            );

                            return new RuntimeException(
                                    "Order not found for bookingId: "
                                            + bookingId
                            );
                        });


        return buildOrderResponse(order);
    }


    // =====================================================
    // BUILD ORDER RESPONSE
    // =====================================================

    private OrderResponse buildOrderResponse(
            Order order
    ) {

        log.debug(
                "Building order response | orderId={}",
                order.getId()
        );


        List<OrderItemResponse> items =
                orderItemRepository
                        .findByOrderId(order.getId())
                        .stream()
                        .map(item ->
                                new OrderItemResponse(
                                        item.getId(),
                                        item.getFoodItemId(),
                                        item.getQuantity(),
                                        item.getPrice(),
                                        item.getSubtotal()
                                )
                        )
                        .toList();


        log.debug(
                "Order response built | orderId={} | itemCount={}",
                order.getId(),
                items.size()
        );


        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getTableId(),
                order.getBookingId(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getCreatedAt(),
                items,
                order.getUpdatedAt()
        );
    }


    // =====================================================
    // CANCEL ORDER
    // =====================================================

    @Transactional
    public void cancelOrder(
            Long orderId,
            Long customerId
    ) {

        log.info(
                "Cancelling order | orderId={} | customerId={}",
                orderId,
                customerId
        );


        Order order =
                orderRepository.findById(orderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Order not found with id: "
                                                + orderId
                                )
                        );


        if (!order.getCustomerId().equals(customerId)) {

            throw new RuntimeException(
                    "You are not authorized to cancel this order"
            );
        }


        if (order.getStatus() != OrderStatus.PLACED) {

            throw new RuntimeException(
                    "Order cannot be cancelled in current status: "
                            + order.getStatus()
            );
        }


        OrderStatus oldStatus =
                order.getStatus();


        order.setStatus(
                OrderStatus.CANCELLED
        );


        orderRepository.save(order);


        log.info(
                "Order cancelled successfully | orderId={} | customerId={} | status={} -> {}",
                orderId,
                customerId,
                oldStatus,
                OrderStatus.CANCELLED
        );
    }


    // =====================================================
    // CANCEL ORDER BY BOOKING
    // =====================================================

    @Transactional
    public void cancelOrderByBooking(
            Long customerId,
            Long tableId,
            Long bookingId
    ) {

        log.info(
                "Cancelling order by booking | customerId={} | tableId={} | bookingId={}",
                customerId,
                tableId,
                bookingId
        );


        Order order =
                orderRepository
                        .findFirstByCustomerIdAndTableIdAndBookingId(
                                customerId,
                                tableId,
                                bookingId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Order not found for bookingId: "
                                                + bookingId
                                )
                        );


        cancelOrder(
                order.getId(),
                customerId
        );
    }


    // =====================================================
    // INTERNAL ORDER CANCELLATION
    // =====================================================

    @Transactional
    public void cancelOrderInternal(
            Long orderId
    ) {

        log.info(
                "Internal order cancellation started | orderId={}",
                orderId
        );


        Order order =
                orderRepository.findById(orderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Order not found with id: "
                                                + orderId
                                )
                        );


        cancelOrderInternal(order);
    }


    private void cancelOrderInternal(
            Order order
    ) {

        if (order.getStatus() != OrderStatus.PLACED) {

            throw new RuntimeException(
                    "Order cannot be cancelled in current status: "
                            + order.getStatus()
            );
        }


        order.setStatus(
                OrderStatus.CANCELLED
        );


        orderRepository.save(order);


        log.info(
                "Internal order cancelled | orderId={}",
                order.getId()
        );
    }


    // =====================================================
    // UPDATE ORDER STATUS
    // =====================================================

    @Transactional
    public OrderResponse updateOrderStatus(
            Long orderId,
            OrderStatus newStatus
    ) {

        log.info(
                "Updating order status | orderId={} | newStatus={}",
                orderId,
                newStatus
        );


        // -------------------------------------------------
        // VALIDATE INPUT
        // -------------------------------------------------

        if (orderId == null) {

            throw new RuntimeException(
                    "Order id is required"
            );
        }


        if (newStatus == null) {

            throw new RuntimeException(
                    "New order status is required"
            );
        }


        // -------------------------------------------------
        // FIND ORDER
        // -------------------------------------------------

        Order order =
                orderRepository.findById(orderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Order not found with id: "
                                                + orderId
                                )
                        );


        OrderStatus currentStatus =
                order.getStatus();


        if (currentStatus == null) {

            throw new RuntimeException(
                    "Order status is missing"
            );
        }


        log.debug(
                "Current order status | orderId={} | currentStatus={} | requestedStatus={}",
                orderId,
                currentStatus,
                newStatus
        );


        // =====================================================
        // TERMINAL STATES
        // =====================================================

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


        // =====================================================
        // RESTAURANT ACCEPTANCE
        // =====================================================

        /*
         * Restaurant can accept a newly placed order.
         *
         * PLACED -> ACCEPTED
         */

        if (
                currentStatus == OrderStatus.PLACED
                        && newStatus == OrderStatus.ACCEPTED
        ) {

            order.setStatus(
                    OrderStatus.ACCEPTED
            );


            order =
                    orderRepository.save(order);


            log.info(
                    "Order accepted by restaurant | orderId={} | {} -> {}",
                    orderId,
                    currentStatus,
                    newStatus
            );


            return buildOrderResponse(order);
        }


        // =====================================================
        // KOT CONTROLLED STATUS
        // =====================================================

        /*
         * PREPARING and READY are controlled through KOT.
         *
         * ACCEPTED -> PREPARING
         * PREPARING -> READY
         */

        if (
                newStatus == OrderStatus.PREPARING
                        || newStatus == OrderStatus.READY
        ) {

            log.warn(
                    "Attempt to update KOT controlled status | orderId={} | currentStatus={} | requestedStatus={}",
                    orderId,
                    currentStatus,
                    newStatus
            );


            throw new RuntimeException(
                    "PREPARING and READY status must be controlled through KOT"
            );
        }


        // =====================================================
        // COMPLETED
        // =====================================================

        /*
         * COMPLETED should happen through the
         * delivery/order completion flow.
         */

        if (newStatus == OrderStatus.COMPLETED) {

            log.warn(
                    "Attempt to complete order through restaurant status API | orderId={} | currentStatus={}",
                    orderId,
                    currentStatus
            );


            throw new RuntimeException(
                    "Order can be completed only through the delivery flow"
            );
        }


        // =====================================================
        // CANCELLED
        // =====================================================

        /*
         * Cancellation is handled separately through:
         *
         * cancelOrder()
         * cancelOrderByBooking()
         * cancelOrderInternal()
         */

        if (newStatus == OrderStatus.CANCELLED) {

            throw new RuntimeException(
                    "Order cancellation must be handled through the cancellation flow"
            );
        }


        // =====================================================
        // INVALID TRANSITION
        // =====================================================

        log.warn(
                "Invalid order status transition | orderId={} | {} -> {}",
                orderId,
                currentStatus,
                newStatus
        );


        throw new RuntimeException(
                "Invalid order status transition: "
                        + currentStatus
                        + " -> "
                        + newStatus
        );
    }


    // =====================================================
    // ADMIN - GET ALL ORDERS
    // =====================================================

    public List<OrderResponse> getAllOrders() {

        log.info(
                "Admin fetching all orders"
        );


        return orderRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::buildOrderResponse)
                .toList();
    }


    // =====================================================
    // ADMIN - GET ORDERS BY STATUS
    // =====================================================

    public List<OrderResponse> getOrdersByStatus(
            OrderStatus status
    ) {

        log.info(
                "Admin fetching orders by status | status={}",
                status
        );


        return orderRepository
                .findByStatusOrderByCreatedAtDesc(status)
                .stream()
                .map(this::buildOrderResponse)
                .toList();
    }


    // =====================================================
    // ADMIN - GET ORDER BY ID
    // =====================================================

    public OrderResponse getOrderByIdForAdmin(
            Long orderId
    ) {

        log.info(
                "Admin fetching order | orderId={}",
                orderId
        );


        Order order =
                orderRepository.findById(orderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Order not found with id: "
                                                + orderId
                                )
                        );


        return buildOrderResponse(order);
    }


    // =====================================================
    // ADMIN - GET TODAY ORDERS
    // =====================================================

    public List<OrderResponse> getTodayOrders() {

        LocalDate today =
                LocalDate.now();


        LocalDateTime start =
                today.atStartOfDay();


        LocalDateTime end =
                today.plusDays(1).atStartOfDay();


        log.info(
                "Admin fetching today's orders | date={}",
                today
        );


        return orderRepository
                .findByCreatedAtBetweenOrderByCreatedAtDesc(
                        start,
                        end
                )
                .stream()
                .map(this::buildOrderResponse)
                .toList();
    }

    // =====================================================
    // ADD DINE-IN ITEMS
    // =====================================================
    @Transactional
    public OrderResponse addDineInItems(
            Long orderId,
            AddDineInItemsRequest request
    ) {

        log.info(
                "Adding dine-in items | orderId={}",
                orderId
        );

        Order order = getOrder(orderId);

        // -------------------------------------------------
        // DINE-IN ONLY
        // -------------------------------------------------

        if (order.getOrderType() != OrderType.DINE_IN) {
            throw new RuntimeException(
                    "Items can be added only for DINE_IN orders"
            );
        }

        // -------------------------------------------------
        // FINAL SUBMIT CHECK
        // -------------------------------------------------

        if (order.isFinalSubmitted()) {
            throw new RuntimeException(
                    "Order is already final submitted. " +
                            "No more items can be added."
            );
        }

        // -------------------------------------------------
        // ORDER STATUS CHECK
        // -------------------------------------------------

        if (
                order.getStatus() == OrderStatus.COMPLETED ||
                        order.getStatus() == OrderStatus.CANCELLED
        ) {
            throw new RuntimeException(
                    "Cannot add items to order with status: "
                            + order.getStatus()
            );
        }

        // -------------------------------------------------
        // ADD ITEMS
        // -------------------------------------------------

        for (AddDineInItemsRequest.ItemRequest itemRequest
                : request.items()) {

            if (
                    itemRequest.foodItemId() == null ||
                            itemRequest.quantity() == null ||
                            itemRequest.quantity() <= 0 ||
                            itemRequest.price() == null ||
                            itemRequest.price().compareTo(BigDecimal.ZERO) < 0
            ) {
                throw new RuntimeException(
                        "Invalid item details"
                );
            }

            BigDecimal subtotal =
                    itemRequest.price()
                            .multiply(
                                    BigDecimal.valueOf(
                                            itemRequest.quantity()
                                    )
                            );

            OrderItem item =
                    OrderItem.builder()
                            .orderId(order.getId())
                            .foodItemId(
                                    itemRequest.foodItemId()
                            )
                            .quantity(
                                    itemRequest.quantity()
                            )
                            .price(
                                    itemRequest.price()
                            )
                            .subtotal(subtotal)
                            .kotId(null)
                            .build();

            orderItemRepository.save(item);
        }

        // -------------------------------------------------
        // RECALCULATE TOTAL
        // -------------------------------------------------

        recalculateOrderTotal(order);

        log.info(
                "Dine-in items added | orderId={}",
                orderId
        );

        return buildOrderResponse(order);
    }


// =====================================================
// SEND UNSENT ITEMS TO KITCHEN
// =====================================================

    @Transactional
    public Long sendItemsToKitchen(
            Long orderId,
            String generatedBy
    ) {

        log.info(
                "Sending dine-in items to kitchen | orderId={}",
                orderId
        );

        Order order = getOrder(orderId);

        // -------------------------------------------------
        // DINE-IN ONLY
        // -------------------------------------------------

        if (order.getOrderType() != OrderType.DINE_IN) {
            throw new RuntimeException(
                    "KOT is allowed only for DINE_IN orders"
            );
        }

        // -------------------------------------------------
        // FINAL SUBMIT CHECK
        // -------------------------------------------------

        if (order.isFinalSubmitted()) {
            throw new RuntimeException(
                    "Order already final submitted"
            );
        }

        // -------------------------------------------------
        // FIND ONLY NEW ITEMS
        // -------------------------------------------------

        List<OrderItem> unsentItems =
                orderItemRepository
                        .findByOrderIdAndKotIdIsNull(
                                orderId
                        );

        if (unsentItems.isEmpty()) {
            throw new RuntimeException(
                    "No new items available to send to kitchen"
            );
        }

        // -------------------------------------------------
        // CREATE KOT
        // -------------------------------------------------

        KOT kot =
                KOT.builder()
                        .orderId(order.getId())
                        .tableId(order.getTableId())
                        .status(KOTStatus.GENERATED)
                        .generatedBy(
                                generatedBy == null ||
                                        generatedBy.isBlank()
                                        ? "STAFF"
                                        : generatedBy
                        )
                        .finalKot(false)
                        .build();

        kot = kotRepository.save(kot);

        // -------------------------------------------------
        // CREATE KOT ITEMS
        // -------------------------------------------------

        for (OrderItem orderItem : unsentItems) {

            KOTItem kotItem =
                    KOTItem.builder()
                            .kotId(kot.getId())
                            .orderItemId(
                                    orderItem.getId()
                            )
                            .foodItemId(
                                    orderItem.getFoodItemId()
                            )
                            .quantity(
                                    orderItem.getQuantity()
                            )
                            .price(
                                    orderItem.getPrice()
                            )
                            .subtotal(
                                    orderItem.getSubtotal()
                            )
                            .build();

            kotItemRepository.save(kotItem);

            // ---------------------------------------------
            // MARK ORDER ITEM AS SENT
            // ---------------------------------------------

            orderItem.setKotId(kot.getId());

            orderItemRepository.save(orderItem);
        }

        // -------------------------------------------------
        // ORDER STATUS
        // -------------------------------------------------

        if (order.getStatus() == OrderStatus.PLACED) {

            order.setStatus(
                    OrderStatus.ACCEPTED
            );

            orderRepository.save(order);
        }

        log.info(
                "KOT created | kotId={} | orderId={} | items={}",
                kot.getId(),
                orderId,
                unsentItems.size()
        );

        return kot.getId();
    }


// =====================================================
// FINAL SUBMIT DINE-IN ORDER
// =====================================================

    @Transactional
    public OrderResponse finalSubmitDineInOrder(
            Long orderId
    ) {

        log.info(
                "Final submitting dine-in order | orderId={}",
                orderId
        );

        Order order = getOrder(orderId);

        // -------------------------------------------------
        // DINE-IN ONLY
        // -------------------------------------------------

        if (order.getOrderType() != OrderType.DINE_IN) {
            throw new RuntimeException(
                    "Final submit is available only for DINE_IN orders"
            );
        }

        // -------------------------------------------------
        // ALREADY SUBMITTED
        // -------------------------------------------------

        if (order.isFinalSubmitted()) {
            throw new RuntimeException(
                    "Order is already final submitted"
            );
        }

        // -------------------------------------------------
        // CHECK ITEMS
        // -------------------------------------------------

        List<OrderItem> allItems =
                orderItemRepository.findByOrderId(orderId);

        if (allItems.isEmpty()) {
            throw new RuntimeException(
                    "Cannot final submit an empty order"
            );
        }

        // -------------------------------------------------
        // CHECK UNSENT ITEMS
        // -------------------------------------------------

        List<OrderItem> unsentItems =
                orderItemRepository
                        .findByOrderIdAndKotIdIsNull(
                                orderId
                        );

        if (!unsentItems.isEmpty()) {

            throw new RuntimeException(
                    "Some items have not been sent to kitchen. " +
                            "Send items to kitchen before final submit."
            );
        }

        // -------------------------------------------------
        // FINAL SUBMIT
        // -------------------------------------------------

        order.setFinalSubmitted(true);

        /*
         * IMPORTANT:
         *
         * Do NOT set READY here.
         *
         * READY means kitchen has completed preparation.
         * FINAL SUBMITTED means customer/staff has finished
         * ordering and no more items can be added.
         */

        recalculateOrderTotal(order);

        order = orderRepository.save(order);

        log.info(
                "Dine-in order final submitted | orderId={} | total={}",
                orderId,
                order.getTotalAmount()
        );

        return buildOrderResponse(order);
    }


// =====================================================
// RECALCULATE ORDER TOTAL
// =====================================================

    private void recalculateOrderTotal(
            Order order
    ) {

        List<OrderItem> items =
                orderItemRepository.findByOrderId(
                        order.getId()
                );

        BigDecimal total =
                items.stream()
                        .map(OrderItem::getSubtotal)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        order.setTotalAmount(total);

        orderRepository.save(order);
    }


// =====================================================
// GET ORDER
// =====================================================

    private Order getOrder(
            Long orderId
    ) {

        return orderRepository
                .findById(orderId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Order not found with id: "
                                        + orderId
                        )
                );
    }

}