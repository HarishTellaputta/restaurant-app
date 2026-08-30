package com.restaurant.order_service.dto;

import com.restaurant.order_service.entity.KOTStatus;

import java.time.LocalDateTime;

public record KOTResponse(

        Long id,

        Long orderId,

        Long tableId,

        KOTStatus status,

        String generatedBy,

        LocalDateTime createdAt,

        LocalDateTime updatedAt

) {
}