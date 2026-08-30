package com.restaurant.order_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.util.List;

public record AddDineInItemsRequest(

        @NotEmpty(message = "At least one item is required")
        @Valid
        List<ItemRequest> items

) {

    public record ItemRequest(

            Long foodItemId,

            Integer quantity,

            BigDecimal price
    ) {
    }
}

