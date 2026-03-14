package com.balians.musicgen.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyEmailChangeRequest(
        @NotBlank(message = "otpCode is required")
        @Pattern(regexp = "\\d{6}", message = "otpCode must be 6 digits")
        String otpCode
) {
}
