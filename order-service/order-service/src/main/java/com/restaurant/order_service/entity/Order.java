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

    @Column(nullable = true)
    private Long customerId;

    // =====================================================
    // DINE-IN / BOOKING
    // =====================================================

    // Optional for DINE_IN
    private Long tableId;

    // Optional for table booking
    private Long bookingId;

    private boolean tableBooking;


    // =====================================================
    // PRICE BREAKDOWN
    // =====================================================

    /**
     * Total price of all order items
     * before discounts and additional charges.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    /**
     * Total discount applied to the order.
     *
     * This can include:
     * - Coupon discount
     * - Offer discount
     * - Additional discount
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discount;

    /**
     * Delivery charge.
     *
     * Mainly applicable for DELIVERY orders.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal deliveryCharge;

    /**
     * Platform/service fee charged by the application.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal platformFee;

    /**
     * Tax/GST amount.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal tax;

    /**
     * Final amount payable by customer.
     *
     * subtotal
     * - discount
     * + deliveryCharge
     * + platformFee
     * + tax
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;


    // =====================================================
    // ORDER STATUS
    // =====================================================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;


    // =====================================================
    // ORDER TYPE
    // =====================================================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType orderType;


    // =====================================================
    // PAYMENT
    // =====================================================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus;


    // =====================================================
    // TIMESTAMPS
    // =====================================================

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;


    // =====================================================
    // FINAL SUBMIT
    // =====================================================

    /**
     * For DINE_IN:
     *
     * false = staff/customer can continue adding items
     * true  = final order submitted
     *
     * For DELIVERY:
     * this flag is not used.
     */
    @Column(nullable = false)
    private boolean finalSubmitted;


    // =====================================================
    // PRE PERSIST
    // =====================================================

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

        if (subtotal == null) {
            subtotal = BigDecimal.ZERO;
        }

        if (discount == null) {
            discount = BigDecimal.ZERO;
        }

        if (deliveryCharge == null) {
            deliveryCharge = BigDecimal.ZERO;
        }

        if (platformFee == null) {
            platformFee = BigDecimal.ZERO;
        }

        if (tax == null) {
            tax = BigDecimal.ZERO;
        }

        if (totalAmount == null) {
            totalAmount = BigDecimal.ZERO;
        }
    }


    // =====================================================
    // PRE UPDATE
    // =====================================================

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}