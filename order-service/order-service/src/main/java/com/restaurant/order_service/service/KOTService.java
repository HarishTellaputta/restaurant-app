package com.restaurant.order_service.service;

import com.restaurant.order_service.client.MenuServiceClient;
import com.restaurant.order_service.dto.FoodItemResponse;
import com.restaurant.order_service.dto.KOTItemResponse;
import com.restaurant.order_service.dto.KOTResponse;
import com.restaurant.order_service.entity.*;
import com.restaurant.order_service.repository.KOTItemRepository;
import com.restaurant.order_service.repository.KOTRepository;
import com.restaurant.order_service.repository.OrderItemRepository;
import com.restaurant.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KOTService {

    private final KOTRepository kotRepository;
    private final KOTItemRepository kotItemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MenuServiceClient menuServiceClient;


    // =====================================================
    // GET OR CREATE DRAFT KOT
    // =====================================================

    @Transactional
    public KOTResponse getOrCreateDraftKOT(
            Long orderId,
            String generatedBy
    ) {

        log.info(
                "KOT DRAFT request received | orderId={} | generatedBy={}",
                orderId,
                generatedBy
        );

        Order order = getOrder(orderId);

        log.info(
                "Order found | orderId={} | orderType={} | status={} | tableId={}",
                order.getId(),
                order.getOrderType(),
                order.getStatus(),
                order.getTableId()
        );

        /*
         * IMPORTANT:
         * KOT is required for both DINE_IN and DELIVERY.
         */
        if (order.getStatus() != OrderStatus.ACCEPTED) {

            log.warn(
                    "KOT DRAFT rejected | orderId={} | currentOrderStatus={}",
                    orderId,
                    order.getStatus()
            );

            throw new RuntimeException(
                    "KOT can be created only for ACCEPTED order"
            );
        }

        KOT kot =
                kotRepository
                        .findByOrderId(orderId)
                        .orElse(null);

        if (kot != null) {

            log.info(
                    "Existing KOT found | kotId={} | orderId={} | status={}",
                    kot.getId(),
                    orderId,
                    kot.getStatus()
            );

            if (kot.getStatus() != KOTStatus.DRAFT) {

                log.warn(
                        "KOT already finalized | kotId={} | orderId={} | status={}",
                        kot.getId(),
                        orderId,
                        kot.getStatus()
                );

                throw new RuntimeException(
                        "KOT is already finalized. Status: "
                                + kot.getStatus()
                );
            }

            log.info(
                    "Returning existing DRAFT KOT | kotId={} | orderId={}",
                    kot.getId(),
                    orderId
            );

            return buildResponse(kot);
        }

        if (generatedBy == null || generatedBy.isBlank()) {
            generatedBy = "STAFF";
        }

        kot =
                KOT.builder()
                        .orderId(order.getId())
                        .tableId(order.getTableId())
                        .status(KOTStatus.DRAFT)
                        .generatedBy(generatedBy)
                        .build();

        kot = kotRepository.save(kot);

        log.info(
                "DRAFT KOT created successfully | kotId={} | orderId={} | orderType={} | tableId={}",
                kot.getId(),
                orderId,
                order.getOrderType(),
                order.getTableId()
        );

        return buildResponse(kot);
    }


    // =====================================================
    // FINAL SUBMIT KOT
    // =====================================================

    @Transactional
    public KOTResponse finalSubmitKOT(
            Long orderId,
            String generatedBy
    ) {

        log.info(
                "KOT FINAL SUBMIT request | orderId={} | generatedBy={}",
                orderId,
                generatedBy
        );

        Order order = getOrder(orderId);

        log.info(
                "Order found for KOT final submit | orderId={} | orderType={} | status={} | tableId={}",
                order.getId(),
                order.getOrderType(),
                order.getStatus(),
                order.getTableId()
        );

        KOT kot =
                kotRepository
                        .findByOrderId(orderId)
                        .orElseThrow(() -> {

                            log.error(
                                    "KOT final submit failed | Draft KOT not found | orderId={}",
                                    orderId
                            );

                            return new RuntimeException(
                                    "Draft KOT not found for order: "
                                            + orderId
                            );
                        });

        log.info(
                "KOT found for final submit | kotId={} | orderId={} | status={}",
                kot.getId(),
                orderId,
                kot.getStatus()
        );

        if (kot.getStatus() != KOTStatus.DRAFT) {

            log.warn(
                    "KOT final submit rejected | kotId={} | currentStatus={}",
                    kot.getId(),
                    kot.getStatus()
            );

            throw new RuntimeException(
                    "KOT cannot be finalized from status: "
                            + kot.getStatus()
            );
        }

        List<OrderItem> orderItems =
                orderItemRepository.findByOrderId(orderId);

        log.info(
                "Order items fetched for KOT | orderId={} | itemCount={}",
                orderId,
                orderItems.size()
        );

        if (orderItems.isEmpty()) {

            log.warn(
                    "KOT final submit rejected | orderId={} | no order items",
                    orderId
            );

            throw new RuntimeException(
                    "Cannot final submit KOT without items"
            );
        }

        kotItemRepository.deleteByKotId(kot.getId());

        log.info(
                "Existing KOT items removed | kotId={}",
                kot.getId()
        );

        for (OrderItem orderItem : orderItems) {

            log.info(
                    "Adding item to KOT | kotId={} | orderItemId={} | foodItemId={} | quantity={}",
                    kot.getId(),
                    orderItem.getId(),
                    orderItem.getFoodItemId(),
                    orderItem.getQuantity()
            );

            KOTItem kotItem =
                    KOTItem.builder()
                            .kotId(kot.getId())
                            .foodItemId(orderItem.getFoodItemId())
                            .quantity(orderItem.getQuantity())
                            .build();

            kotItemRepository.save(kotItem);
        }

        if (generatedBy == null || generatedBy.isBlank()) {
            generatedBy = "STAFF";
        }

        kot.setGeneratedBy(generatedBy);
        kot.setStatus(KOTStatus.GENERATED);
        kot.setSubmittedAt(LocalDateTime.now());

        kot = kotRepository.save(kot);

        log.info(
                "KOT final submitted successfully | kotId={} | orderId={} | status={}",
                kot.getId(),
                orderId,
                kot.getStatus()
        );

        /*
         * Normally order should already be ACCEPTED.
         * Do not change it here if it is already ACCEPTED.
         */
        if (order.getStatus() == OrderStatus.PLACED) {

            log.info(
                    "Order status update during KOT submit | orderId={} | PLACED -> ACCEPTED",
                    orderId
            );

            order.setStatus(OrderStatus.ACCEPTED);

            orderRepository.save(order);
        }

        return buildResponse(kot);
    }


    // =====================================================
    // START PREPARING
    // =====================================================

    @Transactional
    public KOTResponse startPreparing(Long kotId) {

        log.info(
                "KOT PREPARING request | kotId={}",
                kotId
        );

        KOT kot = getKOT(kotId);

        log.info(
                "KOT found | kotId={} | orderId={} | currentStatus={}",
                kot.getId(),
                kot.getOrderId(),
                kot.getStatus()
        );

        if (kot.getStatus() != KOTStatus.GENERATED) {

            log.warn(
                    "KOT PREPARING rejected | kotId={} | currentStatus={}",
                    kotId,
                    kot.getStatus()
            );

            throw new RuntimeException(
                    "KOT cannot move to PREPARING from status: "
                            + kot.getStatus()
            );
        }

        Order order = getOrder(kot.getOrderId());

        log.info(
                "Order found for preparing | orderId={} | orderType={} | status={}",
                order.getId(),
                order.getOrderType(),
                order.getStatus()
        );

        kot.setStatus(KOTStatus.PREPARING);
        kot = kotRepository.save(kot);

        log.info(
                "KOT status changed | kotId={} | GENERATED -> PREPARING",
                kotId
        );

        if (order.getStatus() != OrderStatus.ACCEPTED) {

            log.warn(
                    "Order cannot move to PREPARING | orderId={} | currentStatus={}",
                    order.getId(),
                    order.getStatus()
            );

            throw new RuntimeException(
                    "Order cannot move to PREPARING from status: "
                            + order.getStatus()
            );
        }

        order.setStatus(OrderStatus.PREPARING);
        orderRepository.save(order);

        log.info(
                "Order status changed | orderId={} | ACCEPTED -> PREPARING",
                order.getId()
        );

        return buildResponse(kot);
    }


    // =====================================================
    // MARK READY
    // =====================================================

    @Transactional
    public KOTResponse markReady(Long kotId) {

        log.info(
                "KOT READY request | kotId={}",
                kotId
        );

        KOT kot = getKOT(kotId);

        log.info(
                "KOT found | kotId={} | orderId={} | currentStatus={}",
                kot.getId(),
                kot.getOrderId(),
                kot.getStatus()
        );

        if (kot.getStatus() != KOTStatus.PREPARING) {

            log.warn(
                    "KOT READY rejected | kotId={} | currentStatus={}",
                    kotId,
                    kot.getStatus()
            );

            throw new RuntimeException(
                    "KOT cannot move to READY from status: "
                            + kot.getStatus()
            );
        }

        Order order = getOrder(kot.getOrderId());

        log.info(
                "Order found for READY | orderId={} | orderType={} | status={}",
                order.getId(),
                order.getOrderType(),
                order.getStatus()
        );

        if (order.getStatus() != OrderStatus.PREPARING) {

            log.warn(
                    "Order cannot move to READY | orderId={} | currentStatus={}",
                    order.getId(),
                    order.getStatus()
            );

            throw new RuntimeException(
                    "Order cannot move to READY from status: "
                            + order.getStatus()
            );
        }

        kot.setStatus(KOTStatus.READY);
        kot = kotRepository.save(kot);

        log.info(
                "KOT status changed | kotId={} | PREPARING -> READY",
                kotId
        );

        order.setStatus(OrderStatus.READY);
        orderRepository.save(order);

        log.info(
                "Order status changed | orderId={} | PREPARING -> READY",
                order.getId()
        );

        return buildResponse(kot);
    }


    // =====================================================
    // OTHER METHODS
    // =====================================================

    public KOTResponse getKOTById(Long kotId) {

        log.info(
                "Get KOT by ID | kotId={}",
                kotId
        );

        return buildResponse(getKOT(kotId));
    }


    public KOTResponse getKOTByOrderId(Long orderId) {

        log.info(
                "Get KOT by order | orderId={}",
                orderId
        );

        KOT kot =
                kotRepository
                        .findByOrderId(orderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "KOT not found for order: "
                                                + orderId
                                )
                        );

        return buildResponse(kot);
    }


    public List<KOTResponse> getKOTsByStatus(KOTStatus status) {

        log.info(
                "Get KOTs by status | status={}",
                status
        );

        return kotRepository
                .findByStatusOrderByCreatedAtAsc(status)
                .stream()
                .map(this::buildResponse)
                .toList();
    }


    public List<KOTResponse> getAllKOTs() {

        log.info("Get all KOTs");

        return kotRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::buildResponse)
                .toList();
    }


    private KOT getKOT(Long kotId) {

        return kotRepository
                .findById(kotId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "KOT not found with id: " + kotId
                        )
                );
    }


    private Order getOrder(Long orderId) {

        return orderRepository
                .findById(orderId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Order not found with id: " + orderId
                        )
                );
    }


    private KOTResponse buildResponse(KOT kot) {

        List<KOTItemResponse> items =
                kotItemRepository
                        .findByKotId(kot.getId())
                        .stream()
                        .map(item ->
                                new KOTItemResponse(
                                        item.getId(),
                                        menuServiceClient
                                                .getFoodItem(item.getFoodItemId())
                                                .name(),
                                        item.getQuantity()
                                )
                        )
                        .toList();

        return new KOTResponse(
                kot.getId(),
                kot.getOrderId(),
                kot.getTableId(),
                kot.getStatus(),
                kot.getGeneratedBy(),
                kot.getCreatedAt(),
                kot.getUpdatedAt(),
                kot.getSubmittedAt(),
                items
        );
    }

    @Transactional
    public KOTResponse generateDeliveryKOT(Long orderId, String generatedBy) {

        log.info(
                "Generating DELIVERY KOT | orderId={} | generatedBy={}",
                orderId,
                generatedBy
        );

        // -----------------------------------------------------
        // GET ORDER
        // -----------------------------------------------------

        Order order = getOrder(orderId);

        log.info(
                "Delivery order found | orderId={} | type={} | status={}",
                order.getId(),
                order.getOrderType(),
                order.getStatus()
        );

        // -----------------------------------------------------
        // VALIDATE DELIVERY ORDER
        // -----------------------------------------------------

        if (order.getOrderType() != OrderType.DELIVERY) {

            throw new RuntimeException(
                    "KOT generation for this endpoint is only for DELIVERY orders"
            );
        }

        // -----------------------------------------------------
        // CHECK EXISTING KOT
        // -----------------------------------------------------

        KOT existingKot =
                kotRepository
                        .findByOrderId(orderId)
                        .orElse(null);

        if (existingKot != null) {

            log.info(
                    "KOT already exists | kotId={} | orderId={} | status={}",
                    existingKot.getId(),
                    orderId,
                    existingKot.getStatus()
            );

            return buildResponse(existingKot);
        }

        // -----------------------------------------------------
        // GET ORDER ITEMS
        // -----------------------------------------------------

        List<OrderItem> orderItems =
                orderItemRepository.findByOrderId(orderId);

        if (orderItems.isEmpty()) {

            throw new RuntimeException(
                    "Cannot generate KOT without order items"
            );
        }

        log.info(
                "Order items found | orderId={} | itemCount={}",
                orderId,
                orderItems.size()
        );

        // -----------------------------------------------------
        // GENERATED BY
        // -----------------------------------------------------

        if (generatedBy == null || generatedBy.isBlank()) {
            generatedBy = "RESTAURANT";
        }

        // -----------------------------------------------------
        // CREATE KOT
        // -----------------------------------------------------

        KOT kot =
                KOT.builder()
                        .orderId(order.getId())

                        // DELIVERY has no table
                        .tableId(null)

                        .status(KOTStatus.GENERATED)

                        .generatedBy(generatedBy)

                        .finalKot(false)

                        .submittedAt(LocalDateTime.now())

                        .build();

        kot = kotRepository.save(kot);

        log.info(
                "DELIVERY KOT created | kotId={} | orderId={}",
                kot.getId(),
                orderId
        );

        // -----------------------------------------------------
        // CREATE KOT ITEMS
        // -----------------------------------------------------

        for (OrderItem orderItem : orderItems) {

            KOTItem kotItem =
                    KOTItem.builder()
                            .kotId(kot.getId())
                            .orderItemId(orderItem.getId())
                            .foodItemId(orderItem.getFoodItemId())
                            .price(orderItem.getPrice())
                            .subtotal(orderItem.getSubtotal())
                            .quantity(orderItem.getQuantity())
                            .build();

            kotItemRepository.save(kotItem);

            log.info(
                    "KOT item added | kotId={} | foodItemId={} | quantity={}",
                    kot.getId(),
                    orderItem.getFoodItemId(),
                    orderItem.getQuantity()
            );
        }

        // -----------------------------------------------------
        // ORDER STATUS
        // -----------------------------------------------------

        if (order.getStatus() == OrderStatus.ACCEPTED) {

            log.info(
                    "Delivery order already ACCEPTED | orderId={}",
                    orderId
            );

        } else if (order.getStatus() == OrderStatus.PLACED) {

            order.setStatus(OrderStatus.ACCEPTED);

            orderRepository.save(order);

            log.info(
                    "Delivery order moved PLACED -> ACCEPTED | orderId={}",
                    orderId
            );

        } else {

            log.warn(
                    "KOT generated but order status is {} | orderId={}",
                    order.getStatus(),
                    orderId
            );
        }

        log.info(
                "DELIVERY KOT generation completed | kotId={} | orderId={}",
                kot.getId(),
                orderId
        );

        return buildResponse(kot);
    }

}