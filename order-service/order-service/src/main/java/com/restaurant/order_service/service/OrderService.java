package com.restaurant.order_service.service;

import com.restaurant.order_service.client.MenuServiceClient;
import com.restaurant.order_service.client.RestaurantServiceClient;
import com.restaurant.order_service.dto.*;
import com.restaurant.order_service.entity.*;
import com.restaurant.order_service.repository.*;
import com.restaurant.order_service.specification.OrderSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final DiscountService discountService;
    private final PlatformFeeService platformFeeService;
    private final TaxService taxService;

    private final DeviceTokenRepository deviceTokenRepository;
    private final FirebaseNotificationService firebaseNotificationService;
    private final NotificationService notificationService;


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

        // =====================================================
        // VALIDATE REQUEST
        // =====================================================

        if (request.orderType() == null) {
            throw new RuntimeException(
                    "Order type is required"
            );
        }

        if (request.paymentMethod() == null) {
            throw new RuntimeException(
                    "Payment method is required"
            );
        }

        if (request.items() == null ||
                request.items().isEmpty()) {

            throw new RuntimeException(
                    "Order must contain at least one item"
            );
        }


        // =====================================================
        // TABLE BOOKING
        // =====================================================

        boolean tableBooking =
                Boolean.TRUE.equals(
                        request.tableBooking()
                );

        Long bookingId = null;


        if (tableBooking) {

            // -------------------------------------------------
            // DINE-IN ONLY
            // -------------------------------------------------

            if (request.orderType() != OrderType.DINE_IN) {

                throw new RuntimeException(
                        "Table booking is allowed only for DINE_IN orders"
                );
            }


            // -------------------------------------------------
            // BOOKING DATE
            // -------------------------------------------------

            if (request.bookingDate() == null) {

                throw new RuntimeException(
                        "Booking date is required when table booking is selected"
                );
            }


            // -------------------------------------------------
            // BOOKING TIME
            // -------------------------------------------------

            if (request.bookingTime() == null) {

                throw new RuntimeException(
                        "Booking time is required when table booking is selected"
                );
            }


            // -------------------------------------------------
            // TABLE ID
            // -------------------------------------------------

            if (request.tableId() == null) {

                throw new RuntimeException(
                        "tableId is required when table booking is selected"
                );
            }


            // -------------------------------------------------
            // CREATE TABLE BOOKING REQUEST
            // -------------------------------------------------

            TableBookingRequest tableBookingRequest =
                    new TableBookingRequest(
                            customerId,
                            request.tableId(),
                            request.bookingDate(),
                            request.bookingTime(),
                            4
                    );


            // -------------------------------------------------
            // CALL RESTAURANT SERVICE
            // -------------------------------------------------

            TableBookingResponse tableBookingResponse =
                    restaurantServiceClient.createBooking(
                            tableBookingRequest
                    );


            // -------------------------------------------------
            // STORE BOOKING ID
            // -------------------------------------------------

            if (tableBookingResponse == null) {

                throw new RuntimeException(
                        "Unable to create table booking"
                );
            }

            bookingId =
                    tableBookingResponse.id();

            log.info(
                    "Table booking created | bookingId={} | customerId={} | tableId={}",
                    bookingId,
                    customerId,
                    request.tableId()
            );
        }


        // =====================================================
        // CALCULATE SUBTOTAL
        // =====================================================

        BigDecimal subtotal =
                BigDecimal.ZERO;


        for (
                CreateOrderItemRequest itemRequest
                : request.items()
        ) {

            // -------------------------------------------------
            // GET FOOD ITEM
            // -------------------------------------------------

            FoodItemResponse foodItem =
                    menuServiceClient.getFoodItem(
                            itemRequest.foodItemId()
                    );


            // -------------------------------------------------
            // FOOD ITEM NOT FOUND
            // -------------------------------------------------

            if (foodItem == null) {

                throw new RuntimeException(
                        "Food item not found: "
                                + itemRequest.foodItemId()
                );
            }


            // -------------------------------------------------
            // CHECK AVAILABILITY
            // -------------------------------------------------

            if (!foodItem.available()) {

                throw new RuntimeException(
                        "Food item is currently unavailable: "
                                + foodItem.name()
                );
            }


            // -------------------------------------------------
            // CURRENT FOOD PRICE
            // -------------------------------------------------

            BigDecimal price =
                    BigDecimal.valueOf(
                            foodItem.price()
                    );


            // -------------------------------------------------
            // ITEM SUBTOTAL
            // -------------------------------------------------

            BigDecimal itemSubtotal =
                    price.multiply(
                            BigDecimal.valueOf(
                                    itemRequest.quantity()
                            )
                    );


            // -------------------------------------------------
            // ADD TO ORDER SUBTOTAL
            // -------------------------------------------------

            subtotal =
                    subtotal.add(
                            itemSubtotal
                    );
        }


        log.info(
                "Order subtotal calculated | customerId={} | subtotal={}",
                customerId,
                subtotal
        );


        // =====================================================
        // DELIVERY CHARGE
        // =====================================================

        BigDecimal deliveryCharge =
                BigDecimal.ZERO;


        if (
                request.orderType()
                        == OrderType.DELIVERY
        ) {

            deliveryCharge =
                    deliveryChargeService
                            .calculateDeliveryCharge(
                                    request.distanceKm(),
                                    request.peakTime(),
                                    request.weather()
                            );

            log.info(
                    "Delivery charge calculated | customerId={} | deliveryCharge={}",
                    customerId,
                    deliveryCharge
            );
        }


        // =====================================================
        // DISCOUNT
        // =====================================================

        /*
         * STEP 9
         *
         * Discount / coupon / offer logic
         * will be implemented here.
         *
         * For now:
         */

        BigDecimal couponDiscount =
                discountService.calculateCouponDiscount(
                        request.couponCode(),
                        subtotal
                );

        BigDecimal offerDiscount =
                discountService.calculateOfferDiscount(
                        subtotal
                );

        BigDecimal discount =
                couponDiscount.max(
                        offerDiscount
                );


        // =====================================================
        // PLATFORM FEE
        // =====================================================

        /*
         * STEP 10
         *
         * Platform fee calculation
         * will be implemented here.
         *
         * For now:
         */

        BigDecimal platformFee =
                platformFeeService.calculatePlatformFee(
                        subtotal
                );


        // =====================================================
        // TAX
        // =====================================================

        /*
         * STEP 10
         *
         * Tax calculation
         * will be implemented here.
         *
         * For now:
         */

        BigDecimal taxableAmount =
                subtotal
                        .subtract(discount)
                        .add(deliveryCharge)
                        .add(platformFee);

        BigDecimal tax =
                taxService.calculateTax(
                        taxableAmount
                );


        // =====================================================
        // FINAL TOTAL
        // =====================================================

        BigDecimal totalAmount =
                subtotal
                        .subtract(discount)
                        .add(deliveryCharge)
                        .add(platformFee)
                        .add(tax);


        log.info(
                "Order total calculated | customerId={} | subtotal={} | discount={} | deliveryCharge={} | platformFee={} | tax={} | total={}",
                customerId,
                subtotal,
                discount,
                deliveryCharge,
                platformFee,
                tax,
                totalAmount
        );


        // =====================================================
        // CREATE ORDER
        // =====================================================

        Order order =
                Order.builder()
                        .customerId(customerId)

                        // DINE-IN table
                        .tableId(
                                request.tableId()
                        )

                        // Table booking
                        .bookingId(
                                bookingId
                        )

                        .tableBooking(
                                tableBooking
                        )

                        // Price breakdown
                        .subtotal(
                                subtotal
                        )

                        .discount(
                                discount
                        )

                        .deliveryCharge(
                                deliveryCharge
                        )

                        .platformFee(
                                platformFee
                        )

                        .tax(
                                tax
                        )

                        .totalAmount(
                                totalAmount
                        )

                        // Order information
                        .status(
                                OrderStatus.PLACED
                        )

                        .orderType(
                                request.orderType()
                        )

                        // Payment
                        .paymentMethod(
                                request.paymentMethod()
                        )

                        .paymentStatus(
                                PaymentStatus.PENDING
                        )

                        // DINE-IN:
                        // customer/staff can continue adding items
                        //
                        // DELIVERY:
                        // order is submitted immediately
                        .finalSubmitted(
                                request.orderType()
                                        == OrderType.DELIVERY
                        )

                        .build();


        order =
                orderRepository.save(
                        order
                );


        // =====================================================
        // CREATE ORDER ITEMS
        // =====================================================

        for (
                CreateOrderItemRequest itemRequest
                : request.items()
        ) {

            // -------------------------------------------------
            // GET FOOD ITEM AGAIN
            // -------------------------------------------------

            FoodItemResponse foodItem =
                    menuServiceClient.getFoodItem(
                            itemRequest.foodItemId()
                    );


            if (foodItem == null) {

                throw new RuntimeException(
                        "Food item not found: "
                                + itemRequest.foodItemId()
                );
            }


            if (!foodItem.available()) {

                throw new RuntimeException(
                        "Food item is currently unavailable: "
                                + foodItem.name()
                );
            }


            // -------------------------------------------------
            // CURRENT PRICE
            // -------------------------------------------------

            BigDecimal price =
                    BigDecimal.valueOf(
                            foodItem.price()
                    );


            // -------------------------------------------------
            // ITEM SUBTOTAL
            // -------------------------------------------------

            BigDecimal itemSubtotal =
                    price.multiply(
                            BigDecimal.valueOf(
                                    itemRequest.quantity()
                            )
                    );


            // -------------------------------------------------
            // CREATE ORDER ITEM
            // -------------------------------------------------

            OrderItem item =
                    OrderItem.builder()
                            .orderId(
                                    order.getId()
                            )

                            .foodItemId(
                                    foodItem.id()
                            )

                            .quantity(
                                    itemRequest.quantity()
                            )

                            .price(
                                    price
                            )

                            .subtotal(
                                    itemSubtotal
                            )

                            .kotId(
                                    null
                            )

                            .build();


            orderItemRepository.save(
                    item
            );
        }


        // =====================================================
        // LOG SUCCESS
        // =====================================================

        log.info(
                "Order created successfully | orderId={} | customerId={} | total={}",
                order.getId(),
                customerId,
                order.getTotalAmount()
        );


        // =====================================================
        // RESPONSE
        // =====================================================

        return buildOrderResponse(
                order
        );
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
                        .map(item -> {

                            String foodItemName = null;
                            String imageUrl = null;

                            try {

                                FoodItemResponse foodItem =
                                        menuServiceClient.getFoodItem(
                                                item.getFoodItemId()
                                        );

                                if (foodItem != null) {

                                    foodItemName =
                                            foodItem.name();

                                    imageUrl =
                                            foodItem.imageUrl();
                                }

                            } catch (Exception e) {

                                log.warn(
                                        "Unable to fetch food item details | foodItemId={}",
                                        item.getFoodItemId()
                                );
                            }

                            return new OrderItemResponse(
                                    item.getId(),
                                    item.getFoodItemId(),
                                    foodItemName,
                                    imageUrl,
                                    item.getQuantity(),
                                    item.getPrice(),
                                    item.getSubtotal()
                            );
                        })
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
                order.isTableBooking(),

                // PRICE BREAKDOWN
                order.getSubtotal(),
                order.getDiscount(),
                order.getDeliveryCharge(),
                order.getPlatformFee(),
                order.getTax(),
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
            throw new RuntimeException("Order id is required");
        }

        if (newStatus == null) {
            throw new RuntimeException("New order status is required");
        }

        // -------------------------------------------------
        // FIND ORDER
        // -------------------------------------------------

        Order order =
                orderRepository.findById(orderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Order not found with id: " + orderId
                                )
                        );

        OrderStatus currentStatus = order.getStatus();

        if (currentStatus == null) {
            throw new RuntimeException("Order status is missing");
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
         * PLACED -> ACCEPTED
         */

        if (
                currentStatus == OrderStatus.PLACED
                        && newStatus == OrderStatus.ACCEPTED
        ) {

            order.setStatus(OrderStatus.ACCEPTED);

            order = orderRepository.save(order);

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
         * PREPARING and READY must be controlled through KOT.
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

    public Page<OrderResponse> getAllOrders(
            OrderStatus status,
            OrderType orderType,
            PaymentStatus paymentStatus,
            Pageable pageable
    ) {

        Specification<Order> specification =
                OrderSpecifications.filter(
                        status,
                        orderType,
                        paymentStatus
                );

        return orderRepository
                .findAll(specification, pageable)
                .map(this::buildOrderResponse);
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
                .findTodayOrders(start, end)
                .stream()
                .map(this::buildOrderResponse)
                .toList();
    }

    @Transactional
    public OrderResponse addDineInItems(
            Long orderId,
            AddDineInItemsRequest request
    ) {

        log.info(
                "Adding dine-in items | orderId={}",
                orderId
        );

        // -------------------------------------------------
        // GET ORDER
        // -------------------------------------------------

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

        if (order.getStatus() == OrderStatus.COMPLETED ||
                order.getStatus() == OrderStatus.CANCELLED) {

            throw new RuntimeException(
                    "Cannot add items to order with status: "
                            + order.getStatus()
            );
        }

        // -------------------------------------------------
        // REQUEST VALIDATION
        // -------------------------------------------------

        if (request == null ||
                request.items() == null ||
                request.items().isEmpty()) {

            throw new RuntimeException(
                    "At least one item is required"
            );
        }

        // -------------------------------------------------
        // ADD ITEMS
        // -------------------------------------------------

        for (AddDineInItemsRequest.ItemRequest itemRequest
                : request.items()) {

            if (itemRequest.foodItemId() == null ||
                    itemRequest.quantity() == null ||
                    itemRequest.quantity() <= 0) {

                throw new RuntimeException(
                        "Invalid item details"
                );
            }

            // -------------------------------------------------
            // GET CURRENT FOOD ITEM FROM MENU SERVICE
            // -------------------------------------------------

            FoodItemResponse foodItem =
                    menuServiceClient.getFoodItem(
                            itemRequest.foodItemId()
                    );

            if (foodItem == null) {

                throw new RuntimeException(
                        "Food item not found: "
                                + itemRequest.foodItemId()
                );
            }

            // -------------------------------------------------
            // CHECK AVAILABILITY
            // -------------------------------------------------

            if (!foodItem.available()) {

                throw new RuntimeException(
                        "Food item is currently unavailable: "
                                + foodItem.name()
                );
            }

            // -------------------------------------------------
            // GET PRICE FROM MENU SERVICE
            // -------------------------------------------------

            if (foodItem.price() == null ||
                    foodItem.price() < 0) {

                throw new RuntimeException(
                        "Invalid food item price: "
                                + foodItem.name()
                );
            }

            BigDecimal price =
                    BigDecimal.valueOf(
                            foodItem.price()
                    );

            // -------------------------------------------------
            // CALCULATE SUBTOTAL
            // -------------------------------------------------

            BigDecimal subtotal =
                    price.multiply(
                            BigDecimal.valueOf(
                                    itemRequest.quantity()
                            )
                    );

            // -------------------------------------------------
            // CREATE ORDER ITEM
            // -------------------------------------------------

            OrderItem item =
                    OrderItem.builder()
                            .orderId(order.getId())
                            .foodItemId(foodItem.id())
                            .quantity(itemRequest.quantity())
                            .price(price)
                            .subtotal(subtotal)

                            // Not sent to kitchen yet
                            .kotId(null)

                            .build();

            orderItemRepository.save(item);
        }

        // -------------------------------------------------
        // RECALCULATE ORDER PRICE
        // -------------------------------------------------

        recalculateOrderTotal(order);

        log.info(
                "Dine-in items added successfully | orderId={}",
                orderId
        );

        return buildOrderResponse(order);
    }
    // =====================================================
// FINAL SUBMIT DINE-IN ORDER
// =====================================================

    @Transactional
    public OrderResponse finalSubmitDineInOrder(
            Long orderId
    ) {

        log.info(
                "Final submitting DINE_IN order | orderId={}",
                orderId
        );

        // -------------------------------------------------
        // GET ORDER
        // -------------------------------------------------

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
        // ALREADY FINAL SUBMITTED
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
                        .findByOrderIdAndKotIdIsNull(orderId);

        if (!unsentItems.isEmpty()) {

            throw new RuntimeException(
                    "Some items have not been sent to kitchen. " +
                            "Send all items to kitchen before final submit."
            );
        }

        // -------------------------------------------------
        // FINAL BILL CALCULATION
        // -------------------------------------------------
        //
        // This should calculate:
        //
        // Item Subtotal
        // + Tax
        // + Service Charge
        // + Table Charge (if applicable)
        // + Other Charges
        // - Discount
        // - Coupon Discount
        // - Additional Discount
        //
        // = Final Total
        //
        // -------------------------------------------------

        recalculateDineInOrderTotal(order);

        // -------------------------------------------------
        // LOCK ORDER
        // -------------------------------------------------

        order.setFinalSubmitted(true);

        order = orderRepository.save(order);

        log.info(
                "DINE_IN order final submitted | orderId={} | total={}",
                orderId,
                order.getTotalAmount()
        );

        return buildOrderResponse(order);
    }
// =====================================================
// RECALCULATE ORDER TOTAL
// =====================================================

    private void recalculateOrderTotal(Order order) {

        List<OrderItem> items =
                orderItemRepository.findByOrderId(
                        order.getId()
                );

        // -------------------------------------------------
        // CALCULATE SUBTOTAL
        // -------------------------------------------------

        BigDecimal subtotal =
                items.stream()
                        .map(OrderItem::getSubtotal)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        // -------------------------------------------------
        // EXISTING DISCOUNT
        // -------------------------------------------------

        BigDecimal discount =
                order.getDiscount() != null
                        ? order.getDiscount()
                        : BigDecimal.ZERO;

        // -------------------------------------------------
        // EXISTING DELIVERY CHARGE
        // DINE_IN = ZERO
        // -------------------------------------------------

        BigDecimal deliveryCharge =
                order.getDeliveryCharge() != null
                        ? order.getDeliveryCharge()
                        : BigDecimal.ZERO;

        // -------------------------------------------------
        // PLATFORM FEE
        // -------------------------------------------------

        BigDecimal platformFee =
                platformFeeService.calculatePlatformFee(
                        subtotal
                );

        // -------------------------------------------------
        // TAXABLE AMOUNT
        // -------------------------------------------------

        BigDecimal taxableAmount =
                subtotal
                        .subtract(discount)
                        .add(deliveryCharge)
                        .add(platformFee);

        // -------------------------------------------------
        // TAX
        // -------------------------------------------------

        BigDecimal tax =
                taxService.calculateTax(
                        taxableAmount
                );

        // -------------------------------------------------
        // FINAL TOTAL
        // -------------------------------------------------

        BigDecimal totalAmount =
                subtotal
                        .subtract(discount)
                        .add(deliveryCharge)
                        .add(platformFee)
                        .add(tax);

        // -------------------------------------------------
        // UPDATE ORDER
        // -------------------------------------------------

        order.setSubtotal(subtotal);
        order.setDiscount(discount);
        order.setDeliveryCharge(deliveryCharge);
        order.setPlatformFee(platformFee);
        order.setTax(tax);
        order.setTotalAmount(totalAmount);

        orderRepository.save(order);

        log.info(
                "Dine-in total recalculated | orderId={} | subtotal={} | discount={} | platformFee={} | tax={} | total={}",
                order.getId(),
                subtotal,
                discount,
                platformFee,
                tax,
                totalAmount
        );
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


    @Transactional
    public OrderResponse createOrderFromCart(
            Long customerId,
            CheckoutRequest request
    ) {

        log.info(
                "Creating order from cart | customerId={} | orderType={}",
                customerId,
                request.orderType()
        );

        // =====================================================
        // GET CART
        // =====================================================

        Cart cart =
                cartRepository
                        .findByCustomerId(customerId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Cart not found"
                                )
                        );

        // =====================================================
        // GET CART ITEMS
        // =====================================================

        List<CartItem> cartItems =
                cartItemRepository
                        .findByCartId(cart.getId());

        if (cartItems.isEmpty()) {

            throw new RuntimeException(
                    "Cart is empty"
            );
        }


        // =====================================================
        // VALIDATE ORDER TYPE
        // =====================================================

        if (request.orderType() == null) {

            throw new RuntimeException(
                    "Order type is required"
            );
        }


        if (request.paymentMethod() == null) {

            throw new RuntimeException(
                    "Payment method is required"
            );
        }


//        // =====================================================
//        // TABLE BOOKING
//        // =====================================================
//
//        boolean tableBooking =
//                Boolean.TRUE.equals(
//                        request.tableBooking()
//                );
//
//        Long bookingId = null;
//
//
//        if (tableBooking) {
//
//            if (request.orderType() != OrderType.DINE_IN) {
//
//                throw new RuntimeException(
//                        "Table booking is allowed only for DINE_IN orders"
//                );
//            }
//
//            if (request.bookingDate() == null) {
//
//                throw new RuntimeException(
//                        "Booking date is required"
//                );
//            }
//
//            if (request.bookingTime() == null) {
//
//                throw new RuntimeException(
//                        "Booking time is required"
//                );
//            }
//
//            if (request.tableId() == null) {
//
//                throw new RuntimeException(
//                        "Table ID is required"
//                );
//            }
//
//
//            TableBookingRequest tableBookingRequest =
//                    new TableBookingRequest(
//                            customerId,
//                            request.tableId(),
//                            request.bookingDate(),
//                            request.bookingTime(),
//                            4
//                    );
//
//
//            TableBookingResponse tableBookingResponse =
//                    restaurantServiceClient.createBooking(
//                            tableBookingRequest
//                    );
//
//
//            // IMPORTANT:
//            // Change bookingId() to whatever your
//            // actual TableBookingResponse field is called.
//
//            bookingId =
//                    tableBookingResponse.id();
//        }
//

        // =====================================================
        // CALCULATE SUBTOTAL FROM CART
        // =====================================================

        BigDecimal subtotal =
                cartItems.stream()
                        .map(CartItem::getSubtotal)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );


        // =====================================================
        // DISCOUNT
        // =====================================================

        BigDecimal couponDiscount =
                discountService.calculateCouponDiscount(
                        request.couponCode(),
                        subtotal
                );

        BigDecimal offerDiscount =
                discountService.calculateOfferDiscount(
                        subtotal
                );

        BigDecimal discount =
                discountService.calculateBestDiscount(
                        couponDiscount,
                        offerDiscount
                );

        log.info(
                "Discount calculated | customerId={} | subtotal={} | couponDiscount={} | offerDiscount={} | finalDiscount={}",
                customerId,
                subtotal,
                couponDiscount,
                offerDiscount,
                discount
        );
        // =====================================================
        // DELIVERY CHARGE
        // =====================================================

        BigDecimal deliveryCharge =
                BigDecimal.ZERO;


        if (request.orderType() == OrderType.DELIVERY) {

            deliveryCharge =
                    deliveryChargeService
                            .calculateDeliveryCharge(
                                    request.distanceKm(),
                                    request.peakTime(),
                                    request.weather()
                            );
        }


        // =====================================================
        // PLATFORM FEE
        // =====================================================

        BigDecimal platformFee =
                BigDecimal.ZERO;


        // =====================================================
        // TAX
        // =====================================================

        BigDecimal tax =
                BigDecimal.ZERO;


        // =====================================================
        // FINAL TOTAL
        // =====================================================

        BigDecimal totalAmount =
                subtotal
                        .subtract(discount)
                        .add(deliveryCharge)
                        .add(platformFee)
                        .add(tax);


        // =====================================================
        // CREATE ORDER
        // =====================================================

        Order order =
                Order.builder()
                        .customerId(customerId)
                        .tableId(request.tableId())
                        .bookingId(null)
                        .tableBooking(false)
                        .subtotal(subtotal)
                        .discount(discount)
                        .deliveryCharge(deliveryCharge)
                        .platformFee(platformFee)
                        .tax(tax)
                        .totalAmount(totalAmount)
                        .status(OrderStatus.PLACED)
                        .orderType(request.orderType())
                        .paymentMethod(request.paymentMethod())
                        .paymentStatus(
                                request.paymentMethod()
                                        == PaymentMethod.ONLINE
                                        ? PaymentStatus.PENDING
                                        : PaymentStatus.PENDING
                        )
                        .finalSubmitted(
                                request.orderType()
                                        == OrderType.DELIVERY
                        )
                        .build();


        order =
                orderRepository.save(order);

        // Save notification in database
        List<DeviceToken> adminTokens =
                deviceTokenRepository.findByUserType(UserType.ADMIN);

        for (DeviceToken deviceToken : adminTokens) {

            notificationService.createNotification(
                    deviceToken.getUserId(),
                    "New Food Order 🔔",
                    "New order #" + order.getId() + " has been received.",
                    order.getId()
            );

            // Send FCM notification
            firebaseNotificationService.sendNotification(
                    deviceToken.getToken(),
                    "New Food Order 🔔",
                    "New order #" + order.getId() + " has been received."
            );
        }


        // =====================================================
        // CART → ORDER ITEMS
        // =====================================================

        for (CartItem cartItem : cartItems) {

            // -------------------------------------------------
            // GET CURRENT FOOD ITEM
            // -------------------------------------------------

            FoodItemResponse foodItem =
                    menuServiceClient.getFoodItem(
                            cartItem.getFoodItemId()
                    );


            if (foodItem == null) {

                throw new RuntimeException(
                        "Food item not found: "
                                + cartItem.getFoodItemId()
                );
            }


            if (!foodItem.available()) {

                throw new RuntimeException(
                        "Food item is currently unavailable: "
                                + foodItem.name()
                );
            }


            // -------------------------------------------------
            // IMPORTANT: USE CURRENT MENU PRICE
            // -------------------------------------------------

            BigDecimal currentPrice =
                    BigDecimal.valueOf(
                            foodItem.price()
                    );


            BigDecimal itemSubtotal =
                    currentPrice.multiply(
                            BigDecimal.valueOf(
                                    cartItem.getQuantity()
                            )
                    );


            // -------------------------------------------------
            // CREATE ORDER ITEM
            // -------------------------------------------------

            OrderItem orderItem =
                    OrderItem.builder()
                            .orderId(order.getId())
                            .foodItemId(
                                    cartItem.getFoodItemId()
                            )
                            .quantity(
                                    cartItem.getQuantity()
                            )
                            .price(currentPrice)
                            .subtotal(itemSubtotal)
                            .kotId(null)
                            .build();


            orderItemRepository.save(orderItem);
        }


        // =====================================================
        // CLEAR CART
        // =====================================================

        cartItemRepository.deleteByCartId(
                cart.getId()
        );


        cart.setSubtotal(BigDecimal.ZERO);
        cart.setDiscount(BigDecimal.ZERO);
        cart.setDeliveryCharge(BigDecimal.ZERO);
        cart.setPlatformFee(BigDecimal.ZERO);
        cart.setTax(BigDecimal.ZERO);
        cart.setTotalAmount(BigDecimal.ZERO);

        cartRepository.save(cart);


        log.info(
                "Order created from cart | orderId={} | customerId={} | total={}",
                order.getId(),
                customerId,
                order.getTotalAmount()
        );


        return buildOrderResponse(order);
    }

    // =====================================================
// START DINE-IN ORDER
// =====================================================

    @Transactional
    public OrderResponse startDineInOrder(
            Long customerId
    ) {

        log.info(
                "Starting DINE_IN order | customerId={}",
                customerId
        );

        Order order =
                Order.builder()

                        // Customer is optional for DINE_IN
                        .customerId(customerId)

                        // DINE_IN does not depend on table booking
                        .tableId(null)
                        .bookingId(null)
                        .tableBooking(false)

                        // Initial pricing
                        .subtotal(BigDecimal.ZERO)
                        .discount(BigDecimal.ZERO)
                        .deliveryCharge(BigDecimal.ZERO)
                        .platformFee(BigDecimal.ZERO)
                        .tax(BigDecimal.ZERO)
                        .totalAmount(BigDecimal.ZERO)

                        // Order
                        .orderType(OrderType.DINE_IN)
                        .status(OrderStatus.PLACED)

                        // Payment comes later
                        .paymentMethod(null)
                        .paymentStatus(PaymentStatus.PENDING)

                        // Customer can continue adding items
                        .finalSubmitted(false)

                        .build();

        order =
                orderRepository.save(order);

        log.info(
                "DINE_IN order started | orderId={} | customerId={}",
                order.getId(),
                customerId
        );

        return buildOrderResponse(order);
    }

    // =====================================================
// ATTACH CUSTOMER
// =====================================================

    @Transactional
    public OrderResponse attachCustomer(
            Long orderId,
            Long customerId
    ) {

        log.info(
                "Attaching customer | orderId={} | customerId={}",
                orderId,
                customerId
        );

        Order order = getOrder(orderId);

        if (order.getOrderType() != OrderType.DINE_IN) {

            throw new RuntimeException(
                    "Customer can be attached only to DINE_IN orders"
            );
        }

        if (order.isFinalSubmitted()) {

            throw new RuntimeException(
                    "Cannot attach customer after final submit"
            );
        }

        if (order.getCustomerId() != null) {

            throw new RuntimeException(
                    "Order already has a customer"
            );
        }

        order.setCustomerId(customerId);

        order =
                orderRepository.save(order);

        log.info(
                "Customer attached successfully | orderId={} | customerId={}",
                orderId,
                customerId
        );

        return buildOrderResponse(order);
    }

    // =====================================================
// RECALCULATE DINE-IN BILL
// =====================================================

    private void recalculateDineInOrderTotal(
            Order order
    ) {

        List<OrderItem> items =
                orderItemRepository.findByOrderId(
                        order.getId()
                );

        BigDecimal subtotal =
                items.stream()
                        .map(OrderItem::getSubtotal)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        // -------------------------------------------------
        // DISCOUNT
        // -------------------------------------------------

        BigDecimal discount =
                discountService.calculateOfferDiscount(
                        subtotal
                );

        // -------------------------------------------------
        // DINE-IN DELIVERY = ZERO
        // -------------------------------------------------

        BigDecimal deliveryCharge =
                BigDecimal.ZERO;

        // -------------------------------------------------
        // PLATFORM FEE
        // -------------------------------------------------

        BigDecimal platformFee =
                platformFeeService.calculatePlatformFee(
                        subtotal
                );

        // -------------------------------------------------
        // TAX
        // -------------------------------------------------

        BigDecimal taxableAmount =
                subtotal
                        .subtract(discount)
                        .add(platformFee);

        BigDecimal tax =
                taxService.calculateTax(
                        taxableAmount
                );

        // -------------------------------------------------
        // FINAL TOTAL
        // -------------------------------------------------

        BigDecimal totalAmount =
                subtotal
                        .subtract(discount)
                        .add(deliveryCharge)
                        .add(platformFee)
                        .add(tax);

        // -------------------------------------------------
        // UPDATE ORDER
        // -------------------------------------------------

        order.setSubtotal(subtotal);
        order.setDiscount(discount);
        order.setDeliveryCharge(deliveryCharge);
        order.setPlatformFee(platformFee);
        order.setTax(tax);
        order.setTotalAmount(totalAmount);

        orderRepository.save(order);

        log.info(
                "DINE_IN bill recalculated | orderId={} | subtotal={} | discount={} | platformFee={} | tax={} | total={}",
                order.getId(),
                subtotal,
                discount,
                platformFee,
                tax,
                totalAmount
        );
    }

    // =====================================================
// SEND UNSENT DINE-IN ITEMS TO KITCHEN
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

        // -------------------------------------------------
        // GET ORDER
        // -------------------------------------------------

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
                    "Order is already final submitted"
            );
        }

        // -------------------------------------------------
        // ORDER STATUS CHECK
        // -------------------------------------------------

        if (order.getStatus() == OrderStatus.CANCELLED) {

            throw new RuntimeException(
                    "Cancelled order cannot be sent to kitchen"
            );
        }

        if (order.getStatus() == OrderStatus.COMPLETED) {

            throw new RuntimeException(
                    "Completed order cannot be sent to kitchen"
            );
        }

        // -------------------------------------------------
        // FIND ONLY UNSENT ITEMS
        // kotId = null means item is not yet sent
        // -------------------------------------------------

        List<OrderItem> unsentItems =
                orderItemRepository
                        .findByOrderIdAndKotIdIsNull(orderId);

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
                            .orderItemId(orderItem.getId())
                            .foodItemId(orderItem.getFoodItemId())
                            .quantity(orderItem.getQuantity())
                            .price(orderItem.getPrice())
                            .subtotal(orderItem.getSubtotal())
                            .build();

            kotItemRepository.save(kotItem);

            // -------------------------------------------------
            // MARK ORDER ITEM AS SENT
            // -------------------------------------------------

            orderItem.setKotId(kot.getId());

            orderItemRepository.save(orderItem);
        }

        // -------------------------------------------------
        // ORDER STATUS
        // -------------------------------------------------

        /*
         * First KOT:
         *
         * PLACED → ACCEPTED
         *
         * We don't set PREPARING here.
         * Kitchen/KOT flow controls PREPARING.
         */

        if (order.getStatus() == OrderStatus.PLACED) {

            order.setStatus(
                    OrderStatus.ACCEPTED
            );

            orderRepository.save(order);
        }

        log.info(
                "KOT created successfully | kotId={} | orderId={} | itemCount={}",
                kot.getId(),
                orderId,
                unsentItems.size()
        );

        return kot.getId();
    }

}