package com.restaurant.order_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class DeliveryChargeService {

    public BigDecimal calculateDeliveryCharge(
            Double distanceKm,
            Boolean peakTime,
            String weather
    ) {

        BigDecimal charge = BigDecimal.ZERO;

        try {

            // Distance
            if (distanceKm != null && distanceKm > 0) {

                if (distanceKm <= 2) {

                    charge = charge.add(
                            BigDecimal.valueOf(20)
                    );

                } else {

                    double extraKm =
                            distanceKm - 2;

                    charge = charge.add(
                            BigDecimal.valueOf(
                                    20 + (extraKm * 10)
                            )
                    );
                }
            }

            // Peak time
            if (Boolean.TRUE.equals(peakTime)) {

                charge = charge.add(
                        BigDecimal.valueOf(15)
                );
            }

            // Weather
            if (weather != null &&
                    !weather.isBlank()) {

                if ("RAIN".equalsIgnoreCase(weather)) {

                    charge = charge.add(
                            BigDecimal.valueOf(20)
                    );

                } else if ("EXTREME_HEAT".equalsIgnoreCase(weather)) {

                    charge = charge.add(
                            BigDecimal.valueOf(10)
                    );
                }
            }

        } catch (Exception e) {

            log.warn(
                    "Unable to calculate delivery charge. " +
                            "Continuing with zero delivery charge.",
                    e
            );

            return BigDecimal.ZERO;
        }

        return charge;
    }
}
