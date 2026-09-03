package com.restaurant.order_service.dto;

import com.restaurant.order_service .entity.UserType;

public record SaveTokenRequest(
        Long userId,
        String token,
        UserType userType,
        String platform
) {
}