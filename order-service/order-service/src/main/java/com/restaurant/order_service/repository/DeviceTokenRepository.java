package com.restaurant.order_service.repository;

import com.restaurant.order_service.entity.DeviceToken;
import com.restaurant.order_service.entity.UserType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository
        extends JpaRepository<DeviceToken, Long> {

    Optional<DeviceToken> findByToken(String token);

    List<DeviceToken> findByUserType(UserType userType);

    List<DeviceToken> findByUserId(Long userId);
}