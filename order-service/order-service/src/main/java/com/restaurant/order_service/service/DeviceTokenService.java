package com.restaurant.order_service.service;

import com.restaurant.order_service.dto.SaveTokenRequest;
import com.restaurant.order_service.entity.DeviceToken;
import com.restaurant.order_service.repository.DeviceTokenRepository;
import org.springframework.stereotype.Service;

@Service
public class DeviceTokenService {

    private final DeviceTokenRepository repository;

    public DeviceTokenService(DeviceTokenRepository repository) {
        this.repository = repository;
    }

    public void saveToken(SaveTokenRequest request) {

        DeviceToken token = repository
                .findByToken(request.token())
                .orElse(new DeviceToken());

        token.setUserId(request.userId());
        token.setToken(request.token());
        token.setUserType(request.userType());
        token.setPlatform(request.platform());

        repository.save(token);
    }
}