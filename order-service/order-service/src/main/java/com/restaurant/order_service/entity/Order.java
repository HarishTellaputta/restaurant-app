package com.restaurant.order_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long customerId;

    // Optional for dine-in
    private Long tableId;

    // Optional for pre-booking / celebration orders
    private Long bookingId;

    private boolean tableBooking;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    // =====================================================
    // ORDER TYPE
    // =====================================================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType orderType;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
    /** * For DINE_IN: *
     * * false = staff can continue adding items/KOTs
     * * true = customer finished and staff submitted final order *
     * * For DELIVERY this flag is not used. */
    @Column(nullable = false)
    private boolean finalSubmitted;


    @PrePersist
    protected void onCreate() {

        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (status == null) {
            status = OrderStatus.PLACED;
        }

        if (orderType == null) {
            orderType = OrderType.DINE_IN;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}