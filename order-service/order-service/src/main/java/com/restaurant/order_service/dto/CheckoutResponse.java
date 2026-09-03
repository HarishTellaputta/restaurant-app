package com.restaurant.order_service.dto;

import com.restaurant.order_service.entity.OrderType;
import com.restaurant.order_service.entity.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;

public record CheckoutResponse(

        Long cartId,

        Long customerId,

        OrderType orderType,

        PaymentMethod paymentMethod,

        List<CartItemResponse> items,

        BigDecimal subtotal,

        BigDecimal discount,

        BigDecimal deliveryCharge,

        BigDecimal platformFee,

        BigDecimal tax,

        BigDecimal totalAmount

) {
}