
        package com.restaurant.order_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Slf4j
public class DiscountService {

    // =====================================================
    // COUPON DISCOUNT
    // =====================================================

    public BigDecimal calculateCouponDiscount(
            String couponCode,
            BigDecimal subtotal
    ) {

        if (couponCode == null ||
                couponCode.isBlank()) {

            return BigDecimal.ZERO;
        }

        if (subtotal == null ||
                subtotal.compareTo(BigDecimal.ZERO) <= 0) {

            return BigDecimal.ZERO;
        }

        String coupon =
                couponCode.trim().toUpperCase();

        /*
         * TEMPORARY COUPON LOGIC
         *
         * Later this should come from
         * CouponService / CouponRepository.
         */

        return switch (coupon) {

            // 10% discount, maximum ₹100
            case "WELCOME10" -> {

                BigDecimal discount =
                        subtotal
                                .multiply(
                                        BigDecimal.valueOf(10)
                                )
                                .divide(
                                        BigDecimal.valueOf(100),
                                        2,
                                        RoundingMode.HALF_UP
                                );

                yield discount.min(
                        BigDecimal.valueOf(100)
                );
            }

            // ₹50 discount when subtotal >= ₹300
            case "SAVE50" -> {

                if (
                        subtotal.compareTo(
                                BigDecimal.valueOf(300)
                        ) >= 0
                ) {
                    yield BigDecimal.valueOf(50);
                }

                yield BigDecimal.ZERO;
            }

            default -> {

                log.warn(
                        "Invalid coupon code: {}",
                        couponCode
                );

                yield BigDecimal.ZERO;
            }
        };
    }


    // =====================================================
    // OFFER DISCOUNT
    // =====================================================

    public BigDecimal calculateOfferDiscount(
            BigDecimal subtotal
    ) {

        if (subtotal == null ||
                subtotal.compareTo(BigDecimal.ZERO) <= 0) {

            return BigDecimal.ZERO;
        }

        /*
         * TEMPORARY OFFER LOGIC
         *
         * Later this should come from
         * OfferService / RestaurantService.
         */

        // Example:
        // ₹500 or more → 15% discount
        // maximum ₹150

        if (
                subtotal.compareTo(
                        BigDecimal.valueOf(500)
                ) >= 0
        ) {

            BigDecimal discount =
                    subtotal
                            .multiply(
                                    BigDecimal.valueOf(15)
                            )
                            .divide(
                                    BigDecimal.valueOf(100),
                                    2,
                                    RoundingMode.HALF_UP
                            );

            return discount.min(
                    BigDecimal.valueOf(150)
            );
        }

        return BigDecimal.ZERO;
    }


    // =====================================================
    // SELECT BEST DISCOUNT
    // =====================================================

    public BigDecimal calculateBestDiscount(
            BigDecimal couponDiscount,
            BigDecimal offerDiscount
    ) {

        BigDecimal coupon =
                couponDiscount == null
                        ? BigDecimal.ZERO
                        : couponDiscount;

        BigDecimal offer =
                offerDiscount == null
                        ? BigDecimal.ZERO
                        : offerDiscount;

        /*
         * Coupon and offer are NOT stacked.
         *
         * Customer gets whichever gives
         * the higher discount.
         */

        return coupon.max(offer);
    }
}

