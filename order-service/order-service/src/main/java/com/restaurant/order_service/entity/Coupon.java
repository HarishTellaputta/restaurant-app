package com.restaurant.order_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Coupon code entered by customer.
     */
    @Column(nullable = false, unique = true)
    private String code;

    /**
     * Example:
     *
     * PERCENTAGE
     * FIXED
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;

    /**
     * Example:
     *
     * 10 = 10%
     * 100 = ₹100
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    /**
     * Minimum order subtotal required.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal minimumOrderAmount;

    /**
     * Maximum discount allowed.
     *
     * Mainly useful for percentage coupons.
     *
     * Example:
     * 20% discount
     * maximum ₹200
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal maximumDiscountAmount;

    /**
     * Coupon validity.
     */
    private LocalDateTime startDate;

    private LocalDateTime endDate;

    /**
     * Whether coupon is currently active.
     */
    @Column(nullable = false)
    private boolean active;

    /**
     * Maximum number of times
     * this coupon can be used.
     *
     * null = unlimited.
     */
    private Integer usageLimit;

    /**
     * Number of times coupon already used.
     */
    @Column(nullable = false)
    private Integer usedCount;

    @PrePersist
    protected void onCreate() {

        if (minimumOrderAmount == null) {
            minimumOrderAmount =
                    BigDecimal.ZERO;
        }

        if (usedCount == null) {
            usedCount = 0;
        }

        if (active == false) {
            // intentionally keep false
        }
    }
}