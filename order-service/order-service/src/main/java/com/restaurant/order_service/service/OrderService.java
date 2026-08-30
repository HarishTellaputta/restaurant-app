package com.restaurant.order_service.service;

import com.restaurant.order_service.client.MenuServiceClient;
import com.restaurant.order_service.client.RestaurantServiceClient;
import com.restaurant.order_service.dto.*;
import com.restaurant.order_service.entity.Order;
import com.restaurant.order_service.entity.OrderItem;
import com.restaurant.order_service.entity.OrderStatus;
import com.restaurant.order_service.repository.OrderItemRepository;
import com.restaurant.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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


    // =====================================================
    // CREATE ORDER
    // =====================================================

    @Transactional
    public OrderResponse createOrder(
            Long customerId,
            CreateOrderRequest request
    ) {

        log.info(
                "Creating order | customerId={} | tableId={} | tableBooking={} | itemCount={}",
                customerId,
                request.tableId(),
                request.tableBooking(),
                request.items() != null ? request.items().size() : 0
        );

        Long tableId = request.tableId();
        Boolean tableBooking = request.tableBooking();

        TableBookingResponse booking = null;


        // =====================================================
        // VALIDATE REQUEST
        // =====================================================

        if (request.items() == null || request.items().isEmpty()) {

            throw new RuntimeException(
                    "Order must contain at least one item"
            );
        }


        // =====================================================
        // VALIDATE TABLE BOOKING FLAG
        // =====================================================

        if (tableBooking != null && tableId == null) {

            log.warn(
                    "Invalid order request | tableBooking={} without tableId | customerId={}",
                    tableBooking,
                    customerId
            );

            throw new RuntimeException(
                    "tableId is required when tableBooking is specified"
            );
        }


        // =====================================================
        // TYPE 1:
        // TABLE BOOKING + PREORDER
        // =====================================================

        if (Boolean.TRUE.equals(tableBooking)) {

            // -------------------------------------------------
            // VALIDATE BOOKING DATE
            // -------------------------------------------------

            if (request.bookingDate() == null) {

                throw new RuntimeException(
                        "bookingDate is required for table booking"
                );
            }


            // -------------------------------------------------
            // VALIDATE BOOKING TIME
            // -------------------------------------------------

            if (request.bookingTime() == null) {

                throw new RuntimeException(
                        "bookingTime is required for table booking"
                );
            }


            LocalDate bookingDate = request.bookingDate();
            LocalTime bookingTime = request.bookingTime();


            // -------------------------------------------------
            // VALIDATE BOOKING DATE
            // -------------------------------------------------

            if (bookingDate.isBefore(LocalDate.now())) {

                throw new RuntimeException(
                        "bookingDate cannot be in the past"
                );
            }


            // -------------------------------------------------
            // VALIDATE BOOKING TIME
            // -------------------------------------------------

            if (
                    bookingDate.equals(LocalDate.now())
                            && bookingTime.isBefore(LocalTime.now())
            ) {

                throw new RuntimeException(
                        "bookingTime cannot be in the past"
                );
            }


            // -------------------------------------------------
            // CREATE TABLE BOOKING
            // -------------------------------------------------

            log.info(
                    "Creating table booking | customerId={} | tableId={} | date={} | time={}",
                    customerId,
                    tableId,
                    bookingDate,
                    bookingTime
            );


            try {

                TableBookingRequest bookingRequest =
                        new TableBookingRequest(
                                customerId,
                                tableId,
                                bookingDate,
                                bookingTime,
                                2
                        );


                booking =
                        restaurantServiceClient.createBooking(
                                bookingRequest
                        );


                if (booking == null) {

                    log.error(
                            "Restaurant service returned null booking | customerId={} | tableId={}",
                            customerId,
                            tableId
                    );

                    throw new RuntimeException(
                            "Table booking creation failed"
                    );
                }


                log.info(
                        "Table booking created | bookingId={} | customerId={} | tableId={} | status={}",
                        booking.id(),
                        booking.customerId(),
                        booking.tableId(),
                        booking.status()
                );


            } catch (Exception e) {

                log.error(
                        "Failed to create table booking | customerId={} | tableId={}",
                        customerId,
                        tableId,
                        e
                );

                throw new RuntimeException(
                        "Unable to create table booking: "
                                + e.getMessage(),
                        e
                );
            }


            // -------------------------------------------------
            // CUSTOMER VALIDATION
            // -------------------------------------------------

            if (
                    booking.customerId() == null
                            || !booking.customerId().equals(customerId)
            ) {

                log.warn(
                        "Booking ownership validation failed | bookingCustomerId={} | loggedInCustomerId={}",
                        booking.customerId(),
                        customerId
                );

                throw new RuntimeException(
                        "Created booking does not belong to the logged-in customer"
                );
            }


            // -------------------------------------------------
            // TABLE VALIDATION
            // -------------------------------------------------

            if (
                    booking.tableId() == null
                            || !booking.tableId().equals(tableId)
            ) {

                log.warn(
                        "Booking table validation failed | bookingTableId={} | requestedTableId={}",
                        booking.tableId(),
                        tableId
                );

                throw new RuntimeException(
                        "Created booking does not belong to table: "
                                + tableId
                );
            }


            // -------------------------------------------------
            // STATUS VALIDATION
            // -------------------------------------------------

            if (!"CONFIRMED".equals(booking.status())) {

                log.warn(
                        "Booking was not confirmed | bookingId={} | status={}",
                        booking.id(),
                        booking.status()
                );

                throw new RuntimeException(
                        "Table booking was not confirmed. Current status: "
                                + booking.status()
                );
            }


            log.info(
                    "Table booking validation completed | bookingId={} | tableId={}",
                    booking.id(),
                    tableId
            );
        }


        // =====================================================
        // BOOKING ID
        // =====================================================

        Long bookingId =
                booking != null
                        ? booking.id()
                        : null;


        // =====================================================
        // CREATE ORDER
        // =====================================================

        log.debug(
                "Creating order entity | customerId={} | tableId={} | bookingId={} | tableBooking={}",
                customerId,
                tableId,
                bookingId,
                tableBooking
        );


        Order order =
                Order.builder()
                        .customerId(customerId)
                        .tableId(tableId)
                        .bookingId(bookingId)
                        .totalAmount(BigDecimal.ZERO)
                        .status(OrderStatus.PLACED)
                        .build();


        order = orderRepository.save(order);


        log.info(
                "Order created successfully | orderId={} | customerId={} | tableId={} | bookingId={}",
                order.getId(),
                customerId,
                tableId,
                bookingId
        );


        // =====================================================
        // ADD ORDER ITEMS
        // =====================================================

        BigDecimal total = BigDecimal.ZERO;


        log.debug(
                "Processing order items | orderId={} | itemCount={}",
                order.getId(),
                request.items().size()
        );


        for (CreateOrderItemRequest itemRequest : request.items()) {

            // -------------------------------------------------
            // ITEM VALIDATION
            // -------------------------------------------------

            if (itemRequest == null) {

                throw new RuntimeException(
                        "Order item cannot be null"
                );
            }


            if (itemRequest.foodItemId() == null) {

                throw new RuntimeException(
                        "Food item id is required"
                );
            }


            if (
                    itemRequest.quantity() == null
                            || itemRequest.quantity() <= 0
            ) {

                throw new RuntimeException(
                        "Food item quantity must be greater than zero"
                );
            }


            log.debug(
                    "Fetching food item | orderId={} | foodItemId={} | quantity={}",
                    order.getId(),
                    itemRequest.foodItemId(),
                    itemRequest.quantity()
            );


            FoodItemResponse foodItem;


            // -------------------------------------------------
            // GET FOOD ITEM
            // -------------------------------------------------

            try {

                foodItem =
                        menuServiceClient.getFoodItem(
                                itemRequest.foodItemId()
                        );

            } catch (Exception e) {

                log.error(
                        "Failed to fetch food item | orderId={} | foodItemId={}",
                        order.getId(),
                        itemRequest.foodItemId(),
                        e
                );

                throw new RuntimeException(
                        "Unable to fetch food item: "
                                + itemRequest.foodItemId(),
                        e
                );
            }


            // -------------------------------------------------
            // FOOD ITEM VALIDATION
            // -------------------------------------------------

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


            if (foodItem.price() == null) {

                throw new RuntimeException(
                        "Food item price is missing: "
                                + foodItem.name()
                );
            }


            // -------------------------------------------------
            // PRICE
            // -------------------------------------------------

            BigDecimal price =
                    BigDecimal.valueOf(
                            foodItem.price()
                    );


            BigDecimal subtotal =
                    price.multiply(
                            BigDecimal.valueOf(
                                    itemRequest.quantity()
                            )
                    );


            // -------------------------------------------------
            // CREATE ORDER ITEM
            // -------------------------------------------------

            OrderItem orderItem =
                    OrderItem.builder()
                            .orderId(order.getId())
                            .foodItemId(foodItem.id())
                            .quantity(itemRequest.quantity())
                            .price(price)
                            .subtotal(subtotal)
                            .build();


            orderItemRepository.save(orderItem);


            log.debug(
                    "Order item saved | orderId={} | foodItemId={} | quantity={} | price={} | subtotal={}",
                    order.getId(),
                    foodItem.id(),
                    itemRequest.quantity(),
                    price,
                    subtotal
            );


            total = total.add(subtotal);
        }


        // =====================================================
        // DELIVERY CHARGE
        // =====================================================

        BigDecimal deliveryCharge =
                BigDecimal.ZERO;


        /*
         * TABLE ORDER
         *
         * If customer is dining in the restaurant,
         * there is no delivery charge.
         */

        if (Boolean.TRUE.equals(tableBooking)) {

            log.info(
                    "Table booking order detected | orderId={} | deliveryCharge=0",
                    order.getId()
            );

            deliveryCharge = BigDecimal.ZERO;
        }


        /*
         * DELIVERY ORDER
         *
         * Currently distance/weather services are not available.
         *
         * Therefore:
         *
         * distance = null
         * weather = null
         * peakTime = false
         *
         * Order creation will continue normally.
         *
         * Later these values can come from external services.
         */

        else {

            Double distanceKm = null;
            String weatherCondition = null;
            Boolean peakTime = false;


            // -------------------------------------------------
            // DISTANCE
            // -------------------------------------------------

            try {

                /*
                 * FUTURE:
                 *
                 * distanceKm =
                 *      locationService.calculateDistance(...);
                 *
                 * Currently unavailable.
                 */

                log.debug(
                        "Distance service not available yet | orderId={}",
                        order.getId()
                );

            } catch (Exception e) {

                log.warn(
                        "Unable to calculate delivery distance | orderId={}. Continuing without distance charge.",
                        order.getId(),
                        e
                );

                distanceKm = null;
            }


            // -------------------------------------------------
            // WEATHER
            // -------------------------------------------------

            try {

                /*
                 * FUTURE:
                 *
                 * weatherCondition =
                 *      weatherService.getCurrentWeather(...);
                 *
                 * Currently unavailable.
                 */

                log.debug(
                        "Weather service not available yet | orderId={}",
                        order.getId()
                );

            } catch (Exception e) {

                log.warn(
                        "Unable to fetch weather information | orderId={}. Continuing without weather charge.",
                        order.getId(),
                        e
                );

                weatherCondition = null;
            }


            // -------------------------------------------------
            // PEAK TIME
            // -------------------------------------------------

            try {

                /*
                 * FUTURE:
                 *
                 * peakTime =
                 *      deliveryPricingService.isPeakTime(...);
                 *
                 * Currently unavailable.
                 */

                peakTime = false;

                log.debug(
                        "Peak-time service not available yet | orderId={}",
                        order.getId()
                );

            } catch (Exception e) {

                log.warn(
                        "Unable to determine peak time | orderId={}. Continuing without peak charge.",
                        order.getId(),
                        e
                );

                peakTime = false;
            }


            // =================================================
            // BASE DELIVERY CHARGE
            // =================================================

            BigDecimal baseDeliveryCharge =
                    BigDecimal.ZERO;


            /*
             * Only calculate distance charge when
             * distance information is available.
             */

            if (distanceKm != null && distanceKm > 0) {

                /*
                 * FUTURE PRICING EXAMPLE:
                 *
                 * First 3 KM  = ₹30
                 * Every extra KM = ₹10
                 *
                 * Keep this logic in a separate
                 * DeliveryPricingService later.
                 */

                if (distanceKm <= 3) {

                    baseDeliveryCharge =
                            BigDecimal.valueOf(30);

                } else {

                    double extraKm =
                            distanceKm - 3;

                    baseDeliveryCharge =
                            BigDecimal.valueOf(30)
                                    .add(
                                            BigDecimal.valueOf(
                                                    extraKm * 10
                                            )
                                    );
                }
            }


            // =================================================
            // PEAK CHARGE
            // =================================================

            BigDecimal peakCharge =
                    BigDecimal.ZERO;


            if (Boolean.TRUE.equals(peakTime)) {

                /*
                 * FUTURE:
                 *
                 * Example peak charge = ₹20
                 */

                peakCharge =
                        BigDecimal.valueOf(20);
            }


            // =================================================
            // WEATHER CHARGE
            // =================================================

            BigDecimal weatherCharge =
                    BigDecimal.ZERO;


            if (weatherCondition != null) {

                String weather =
                        weatherCondition.toLowerCase();


                /*
                 * FUTURE:
                 *
                 * Rain = ₹20
                 * Heavy rain = ₹30
                 * Extreme heat = ₹10
                 *
                 * Keep pricing configurable later.
                 */

                if (
                        weather.contains("rain")
                                || weather.contains("storm")
                ) {

                    weatherCharge =
                            BigDecimal.valueOf(20);
                }
            }


            // =================================================
            // TOTAL DELIVERY CHARGE
            // =================================================

            deliveryCharge =
                    baseDeliveryCharge
                            .add(peakCharge)
                            .add(weatherCharge);


            log.info(
                    "Delivery charge calculated | orderId={} | distance={} | weather={} | peakTime={} | base={} | peak={} | weatherCharge={} | total={}",
                    order.getId(),
                    distanceKm,
                    weatherCondition,
                    peakTime,
                    baseDeliveryCharge,
                    peakCharge,
                    weatherCharge,
                    deliveryCharge
            );
        }


        // =====================================================
        // FINAL ORDER TOTAL
        // =====================================================

        BigDecimal grandTotal =
                total.add(deliveryCharge);


        // =====================================================
        // UPDATE ORDER TOTAL
        // =====================================================

        order.setTotalAmount(grandTotal);


        /*
         * IMPORTANT:
         *
         * If your Order entity already has:
         *
         * private BigDecimal deliveryCharge;
         *
         * then uncomment:
         *
         * order.setDeliveryCharge(deliveryCharge);
         */


        order =
                orderRepository.save(order);


        log.info(
                "Order total updated | orderId={} | itemTotal={} | deliveryCharge={} | grandTotal={}",
                order.getId(),
                total,
                deliveryCharge,
                grandTotal
        );


        // =====================================================
        // COMPLETE
        // =====================================================

        log.info(
                "Order creation completed successfully | orderId={} | customerId={} | bookingId={} | itemTotal={} | deliveryCharge={} | totalAmount={}",
                order.getId(),
                customerId,
                bookingId,
                total,
                deliveryCharge,
                grandTotal
        );


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
                items
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


        if (!isValidTransition(
                currentStatus,
                newStatus
        )) {

            throw new RuntimeException(
                    "Invalid order status transition: "
                            + currentStatus
                            + " -> "
                            + newStatus
            );
        }


        order.setStatus(newStatus);


        order =
                orderRepository.save(order);


        log.info(
                "Order status updated successfully | orderId={} | {} -> {}",
                orderId,
                currentStatus,
                newStatus
        );


        return buildOrderResponse(order);
    }


    // =====================================================
    // ADMIN - GET ALL ORDERS
    // =====================================================

    public List<OrderResponse> getAllOrders() {

        log.info("Admin fetching all orders");


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
    // VALID STATUS TRANSITION
    // =====================================================

    private boolean isValidTransition(
            OrderStatus currentStatus,
            OrderStatus newStatus
    ) {

        if (currentStatus == null || newStatus == null) {
            return false;
        }

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
            case CANCELLED:
                return false;

            default:
                return false;
        }
    }
}

