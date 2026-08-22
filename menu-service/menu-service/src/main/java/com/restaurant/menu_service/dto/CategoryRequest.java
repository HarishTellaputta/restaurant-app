package com.restaurant.menu_service.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(

        @NotBlank
        String name,

        String description,

        Boolean active

) {
}