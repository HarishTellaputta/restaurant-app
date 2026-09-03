
        package com.restaurant.order_service.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PlatformFeeService {

    private static final BigDecimal PLATFORM_FEE =
            new BigDecimal("10.00");

    public BigDecimal calculatePlatformFee(
            BigDecimal subtotal
    ) {

        if (subtotal == null ||
                subtotal.compareTo(BigDecimal.ZERO) <= 0) {

            return BigDecimal.ZERO;
        }

        return PLATFORM_FEE;
    }
}

