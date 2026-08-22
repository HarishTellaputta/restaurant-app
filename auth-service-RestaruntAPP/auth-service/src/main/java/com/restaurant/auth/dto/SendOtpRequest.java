package com.restaurant.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendOtpRequest {

    @NotBlank
    @Size(min = 10, max = 15)
    private String mobile;
}