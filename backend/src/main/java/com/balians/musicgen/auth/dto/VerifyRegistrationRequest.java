package com.balians.musicgen.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyRegistrationRequest(
        @Email(message = "email must be valid")
        @NotBlank(message = "email is required")
        String email,
        @NotBlank(message = "otpCode is required")
        @Pattern(regexp = "\\d{6}", message = "otpCode must be 6 digits")
        String otpCode
) {
}
