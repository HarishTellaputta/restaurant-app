package com.restaurant.order_service.service;

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


    // =====================================================
    // GET OR CREATE DRAFT KOT
    // =====================================================

    @Transactional
    public KOTResponse getOrCreateDraftKOT(
            Long orderId,
            String generatedBy
    ) {

        Order order = getOrder(orderId);

        validateDineInOrder(order);

        KOT kot =
                kotRepository
                        .findByOrderId(orderId)
                        .orElse(null);

        if (kot != null) {

            if (kot.getStatus() != KOTStatus.DRAFT) {

                throw new RuntimeException(
                        "KOT is already finalized. Status: "
                                + kot.getStatus()
                );
            }

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
                "Draft KOT created | kotId={} | orderId={}",
                kot.getId(),
                orderId
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
                "Final submitting KOT | orderId={}",
                orderId
        );

        Order order = getOrder(orderId);

        validateDineInOrder(order);

        KOT kot =
                kotRepository
                        .findByOrderId(orderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Draft KOT not found for order: "
                                                + orderId
                                )
                        );

        if (kot.getStatus() != KOTStatus.DRAFT) {

            throw new RuntimeException(
                    "KOT cannot be finalized from status: "
                            + kot.getStatus()
            );
        }

        List<OrderItem> orderItems =
                orderItemRepository.findByOrderId(orderId);

        if (orderItems.isEmpty()) {

            throw new RuntimeException(
                    "Cannot final submit KOT without items"
            );
        }

        /*
         * Remove existing KOT items.
         *
         * This makes the KOT contain the latest complete
         * order items at final submission.
         */
        kotItemRepository.deleteByKotId(kot.getId());

        for (OrderItem orderItem : orderItems) {

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

        kot.setStatus(
                KOTStatus.GENERATED
        );

        kot.setSubmittedAt(
                LocalDateTime.now()
        );

        kot = kotRepository.save(kot);

        /*
         * Order moves to ACCEPTED first.
         * Then kitchen can move it to PREPARING.
         */
        if (order.getStatus() == OrderStatus.PLACED) {

            order.setStatus(
                    OrderStatus.ACCEPTED
            );

            orderRepository.save(order);
        }

        log.info(
                "KOT final submitted | kotId={} | orderId={}",
                kot.getId(),
                orderId
        );

        return buildResponse(kot);
    }


    // =====================================================
    // START PREPARING
    // =====================================================

    @Transactional
    public KOTResponse startPreparing(
            Long kotId
    ) {

        KOT kot = getKOT(kotId);

        if (kot.getStatus() != KOTStatus.GENERATED) {

            throw new RuntimeException(
                    "KOT cannot move to PREPARING from status: "
                            + kot.getStatus()
            );
        }

        Order order =
                getOrder(kot.getOrderId());

        validateDineInOrder(order);

        kot.setStatus(
                KOTStatus.PREPARING
        );

        kot = kotRepository.save(kot);

        if (order.getStatus() != OrderStatus.ACCEPTED) {

            throw new RuntimeException(
                    "Order cannot move to PREPARING from status: "
                            + order.getStatus()
            );
        }

        order.setStatus(
                OrderStatus.PREPARING
        );

        orderRepository.save(order);

        log.info(
                "KOT PREPARING | kotId={} | orderId={}",
                kotId,
                order.getId()
        );

        return buildResponse(kot);
    }


    // =====================================================
    // MARK READY
    // =====================================================

    @Transactional
    public KOTResponse markReady(
            Long kotId
    ) {

        KOT kot = getKOT(kotId);

        if (kot.getStatus() != KOTStatus.PREPARING) {

            throw new RuntimeException(
                    "KOT cannot move to READY from status: "
                            + kot.getStatus()
            );
        }

        Order order =
                getOrder(kot.getOrderId());

        validateDineInOrder(order);

        kot.setStatus(
                KOTStatus.READY
        );

        kot = kotRepository.save(kot);

        if (order.getStatus() != OrderStatus.PREPARING) {

            throw new RuntimeException(
                    "Order cannot move to READY from status: "
                            + order.getStatus()
            );
        }

        order.setStatus(
                OrderStatus.READY
        );

        orderRepository.save(order);

        log.info(
                "KOT READY | kotId={} | orderId={}",
                kotId,
                order.getId()
        );

        return buildResponse(kot);
    }


    // =====================================================
    // GET KOT BY ID
    // =====================================================

    public KOTResponse getKOTById(
            Long kotId
    ) {

        return buildResponse(
                getKOT(kotId)
        );
    }


    // =====================================================
    // GET KOT BY ORDER
    // =====================================================

    public KOTResponse getKOTByOrderId(
            Long orderId
    ) {

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


    // =====================================================
    // GET KOTS BY STATUS
    // =====================================================

    public List<KOTResponse> getKOTsByStatus(
            KOTStatus status
    ) {

        return kotRepository
                .findByStatusOrderByCreatedAtAsc(status)
                .stream()
                .map(this::buildResponse)
                .toList();
    }


    // =====================================================
    // GET ALL KOTS
    // =====================================================

    public List<KOTResponse> getAllKOTs() {

        return kotRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::buildResponse)
                .toList();
    }


    // =====================================================
    // GET KOT
    // =====================================================

    private KOT getKOT(
            Long kotId
    ) {

        return kotRepository
                .findById(kotId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "KOT not found with id: "
                                        + kotId
                        )
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


    // =====================================================
    // VALIDATE DINE-IN
    // =====================================================

    private void validateDineInOrder(
            Order order
    ) {

        if (order.getOrderType() != OrderType.DINE_IN) {

            throw new RuntimeException(
                    "KOT is supported only for DINE_IN orders"
            );
        }

        if (order.getTableId() == null) {

            throw new RuntimeException(
                    "DINE_IN order must have a table"
            );
        }
    }


    // =====================================================
    // BUILD RESPONSE
    // =====================================================

    private KOTResponse buildResponse(
            KOT kot
    ) {

        List<KOTItemResponse> items =
                kotItemRepository
                        .findByKotId(kot.getId())
                        .stream()
                        .map(item ->
                                new KOTItemResponse(
                                        item.getId(),
                                        item.getFoodItemId(),
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
}