package com.restaurant.order_service.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "kots",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_kot_order",
                        columnNames = "order_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KOT {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Order associated with this KOT.
     */
    @Column(
            name = "order_id",
            nullable = false
    )
    private Long orderId;

    /**
     * Restaurant table.
     * Null for delivery orders.
     */
    private Long tableId;

    /**
     * Current KOT status.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KOTStatus status;

    /**
     * Who generated the KOT.
     *
     * Example:
     * MANAGER
     * RECEPTION
     * POS
     */
    private String generatedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;


    @PrePersist
    protected void onCreate() {

        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (status == null) {
            status = KOTStatus.GENERATED;
        }
    }


    @PreUpdate
    protected void onUpdate() {

        updatedAt = LocalDateTime.now();
    }
}