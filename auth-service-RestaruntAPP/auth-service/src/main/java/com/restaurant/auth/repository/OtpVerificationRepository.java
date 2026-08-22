package com.restaurant.auth.repository;

import com.restaurant.auth.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpVerificationRepository
        extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findTopByMobileOrderByCreatedAtDesc(String mobile);
}