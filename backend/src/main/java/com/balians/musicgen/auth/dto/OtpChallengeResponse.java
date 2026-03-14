package com.balians.musicgen.auth.dto;

import java.time.Instant;

public record OtpChallengeResponse(
        String email,
        String purpose,
        Instant expiresAt,
        String devOtpPreview
) {
}
