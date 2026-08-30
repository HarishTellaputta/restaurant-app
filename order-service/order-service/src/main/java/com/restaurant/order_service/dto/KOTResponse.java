package com.restaurant.order_service.dto;

import com.restaurant.order_service.entity.KOTStatus;

import java.time.LocalDateTime;
import java.util.List;

public record KOTResponse(

        Long id,

        Long orderId,

        Long tableId,

        KOTStatus status,

        String generatedBy,

        LocalDateTime createdAt,

        LocalDateTime updatedAt,

        LocalDateTime submittedAt,

        List<KOTItemResponse> items
) {
}