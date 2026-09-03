package com.restaurant.order_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record AddDineInItemsRequest(

        @NotEmpty
        List<@Valid ItemRequest> items

) {

    public record ItemRequest(

            @NotNull
            Long foodItemId,

            @NotNull
            @Positive
            Integer quantity

    ) {
    }
}