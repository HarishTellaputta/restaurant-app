package com.restaurant.order_service.specification;

import com.restaurant.order_service.entity.Order;
import com.restaurant.order_service.entity.OrderStatus;
import com.restaurant.order_service.entity.OrderType;
import com.restaurant.order_service.entity.PaymentStatus;
import org.springframework.data.jpa.domain.Specification;

public final class OrderSpecifications {

    private OrderSpecifications() {
    }

    public static Specification<Order> filter(
            OrderStatus status,
            OrderType orderType,
            PaymentStatus paymentStatus
    ) {

        return Specification
                .where(hasStatus(status))
                .and(hasOrderType(orderType))
                .and(hasPaymentStatus(paymentStatus));
    }

    private static Specification<Order> hasStatus(
            OrderStatus status
    ) {

        return (root, query, criteriaBuilder) -> {

            if (status == null) {
                return null;
            }

            return criteriaBuilder.equal(
                    root.get("status"),
                    status
            );
        };
    }

    private static Specification<Order> hasOrderType(
            OrderType orderType
    ) {

        return (root, query, criteriaBuilder) -> {

            if (orderType == null) {
                return null;
            }

            return criteriaBuilder.equal(
                    root.get("orderType"),
                    orderType
            );
        };
    }

    private static Specification<Order> hasPaymentStatus(
            PaymentStatus paymentStatus
    ) {

        return (root, query, criteriaBuilder) -> {

            if (paymentStatus == null) {
                return null;
            }

            return criteriaBuilder.equal(
                    root.get("paymentStatus"),
                    paymentStatus
            );
        };
    }
}