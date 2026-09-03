package com.restaurant.order_service.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(

        Long id,

        Long customerId,

        List<CartItemResponse> items,

        BigDecimal subtotal,

        BigDecimal discount,

        BigDecimal deliveryCharge,

        BigDecimal platformFee,

        BigDecimal tax,

        BigDecimal totalAmount

) {
}