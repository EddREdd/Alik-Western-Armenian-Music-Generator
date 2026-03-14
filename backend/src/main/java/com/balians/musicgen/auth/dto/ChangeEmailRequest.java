package com.balians.musicgen.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ChangeEmailRequest(
        @Email(message = "newEmail must be valid")
        @NotBlank(message = "newEmail is required")
        String newEmail
) {
}
