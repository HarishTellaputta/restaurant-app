package com.restaurant.auth.controller;

import com.restaurant.auth.dto.LoginResponse;
import com.restaurant.auth.dto.SendOtpRequest;
import com.restaurant.auth.dto.VerifyOtpRequest;
import com.restaurant.auth.entity.User;
import com.restaurant.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/send-otp")
    public String sendOtp(
            @Valid @RequestBody SendOtpRequest request) {

        return authService.sendOtp(request.getMobile());
    }

    @PostMapping("/verify-otp")
    public LoginResponse verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {

        return authService.verifyOtp(
                request.getMobile(),
                request.getOtp()
        );
    }

    @GetMapping("/me")
    public User getCurrentUser(Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();

        return authService.getUserById(userId);
    }
}