
        package com.restaurant.order_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "kot_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KOTItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * KOT to which this item belongs.
     */
    @Column(nullable = false)
    private Long kotId;

    /**
     * Original order item.
     */
    @Column(nullable = false)
    private Long orderItemId;

    /**
     * Food item ID.
     */
    @Column(nullable = false)
    private Long foodItemId;

    /**
     * Quantity ordered in this KOT.
     */
    @Column(nullable = false)
    private Integer quantity;

    /**
     * Price at the time of ordering.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * Item subtotal.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;
}
