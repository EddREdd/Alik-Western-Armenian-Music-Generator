package com.balians.musicgen.auth.dto;

import java.time.Instant;

public record AuthSessionResponse(
        AuthUserResponse user,
        String sessionToken,
        Instant sessionExpiresAt
) {
}
