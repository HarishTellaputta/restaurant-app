
        package com.restaurant.order_service.entity;

import com.restaurant.order_service.entity.KOTStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "kots")
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
     * Dine-in order associated with this KOT.
     *
     * One order can have multiple KOTs.
     */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /**
     * Restaurant table.
     */
    private Long tableId;

    /**
     * KOT status.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KOTStatus status;

    /**
     * Who generated the KOT.
     *
     * Examples:
     * STAFF
     * POS
     * ADMIN
     */
    private String generatedBy;

    /**
     * Indicates whether this is the final KOT
     * generated when staff completes the dine-in order.
     */
    @Column(nullable = false)
    private boolean finalKot;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * Time when this KOT was submitted to kitchen.
     */
    private LocalDateTime submittedAt;

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

