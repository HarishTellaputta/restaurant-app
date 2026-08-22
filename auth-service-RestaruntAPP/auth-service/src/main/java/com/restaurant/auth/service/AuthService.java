package com.restaurant.auth.service;

import com.restaurant.auth.dto.LoginResponse;
import com.restaurant.auth.entity.OtpVerification;
import com.restaurant.auth.entity.User;
import com.restaurant.auth.entity.Role;
import com.restaurant.auth.repository.OtpVerificationRepository;
import com.restaurant.auth.repository.UserRepository;
import com.restaurant.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final JwtService jwtService;

    public String sendOtp(String mobile) {

        log.info("OTP generation started for mobile: {}", mobile);

        // Generate 6 digit OTP
        String otp = String.valueOf(
                100000 + new Random().nextInt(900000)
        );

        // Development only
        otp = "123456";

        OtpVerification otpVerification = OtpVerification.builder()
                .mobile(mobile)
                .otp(otp)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .verified(false)
                .build();

        otpVerificationRepository.save(otpVerification);

        log.info("OTP saved successfully for mobile: {}", mobile);

        // Development only
        System.out.println("OTP for " + mobile + " = " + otp);

        log.info("OTP sent successfully for mobile: {}", mobile);

        return "OTP sent successfully";
    }


    public LoginResponse verifyOtp(String mobile, String otp) {

        log.info("OTP verification started for mobile: {}", mobile);

        OtpVerification otpVerification =
                otpVerificationRepository
                        .findTopByMobileOrderByCreatedAtDesc(mobile)
                        .orElseThrow(() -> {
                            log.warn("OTP not found for mobile: {}", mobile);
                            return new RuntimeException("OTP not found");
                        });

        if (otpVerification.getExpiresAt().isBefore(LocalDateTime.now())) {

            log.warn("OTP expired for mobile: {}", mobile);

            throw new RuntimeException("OTP expired");
        }

        if (!otpVerification.getOtp().equals(otp)) {

            log.warn("Invalid OTP entered for mobile: {}", mobile);

            throw new RuntimeException("Invalid OTP");
        }

        log.info("OTP verified successfully for mobile: {}", mobile);

        otpVerification.setVerified(true);
        otpVerificationRepository.save(otpVerification);

        User user = userRepository.findByMobile(mobile)
                .orElseGet(() -> {

                    log.info("New customer. Creating user for mobile: {}", mobile);

                    User newUser = User.builder()
                            .mobile(mobile)
                            .role(Role.CUSTOMER)
                            .active(true)
                            .build();

                    User savedUser = userRepository.save(newUser);

                    log.info("New customer created successfully. User ID: {}",
                            savedUser.getId());

                    return savedUser;
                });

        log.info("Customer login successful. User ID: {}, Role: {}",
                user.getId(),
                user.getRole());

        String accessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getMobile(),
                user.getRole().name()
        );

        log.info("JWT access token generated for user ID: {}", user.getId());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .mobile(user.getMobile())
                .role(user.getRole().name())
                .build();
    }

    public User getUserById(Long userId) {

        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}