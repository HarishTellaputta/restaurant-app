package com.restaurant.order_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "offers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Offer name displayed to customer.
     *
     * Example:
     *
     * Weekend Special
     * Lunch Offer
     * First Order Offer
     */
    @Column(nullable = false)
    private String name;

    /**
     * Offer description.
     */
    private String description;

    /**
     * PERCENTAGE or FIXED.
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
     * Minimum subtotal required.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal minimumOrderAmount;

    /**
     * Maximum discount.
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal maximumDiscountAmount;

    /**
     * Offer validity.
     */
    private LocalDateTime startDate;

    private LocalDateTime endDate;

    /**
     * Offer active/inactive.
     */
    @Column(nullable = false)
    private boolean active;

    @PrePersist
    protected void onCreate() {

        if (minimumOrderAmount == null) {
            minimumOrderAmount =
                    BigDecimal.ZERO;
        }
    }
}