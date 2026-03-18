package com.balians.musicgen.email;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendTestEmailRequest(
        @Email(message = "to must be a valid email")
        @NotBlank(message = "to is required")
        String to,
        @NotBlank(message = "subject is required")
        String subject,
        @NotBlank(message = "body is required")
        String body
) {
}

