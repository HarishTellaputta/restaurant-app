
package com.restaurant.order_service.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class TaxService {

    private static final BigDecimal TAX_RATE =
            new BigDecimal("0.05");

    public BigDecimal calculateTax(
            BigDecimal taxableAmount
    ) {

        if (taxableAmount == null ||
                taxableAmount.compareTo(BigDecimal.ZERO) <= 0) {

            return BigDecimal.ZERO;
        }

        return taxableAmount
                .multiply(TAX_RATE)
                .setScale(
                        2,
                        RoundingMode.HALF_UP
                );
    }
}

