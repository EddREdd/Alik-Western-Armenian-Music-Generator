package com.balians.musicgen.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForgotPasswordResetRequest(
        @Email(message = "email must be valid")
        @NotBlank(message = "email is required")
        String email,
        @NotBlank(message = "resetToken is required")
        String resetToken,
        @NotBlank(message = "newPassword is required")
        @Size(min = 8, max = 100, message = "newPassword must be between 8 and 100 characters")
        String newPassword,
        @NotBlank(message = "confirmPassword is required")
        String confirmPassword
) {
}
