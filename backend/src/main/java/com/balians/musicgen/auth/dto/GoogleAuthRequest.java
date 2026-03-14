package com.balians.musicgen.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleAuthRequest(
        @NotBlank(message = "idToken is required")
        String idToken,
        String inviteCode
) {
}
