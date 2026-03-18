package com.balians.musicgen.auth.dto;

import com.balians.musicgen.generation.dto.GenerationJobResponse;
import java.time.Instant;
import java.util.List;

public record AuthSessionResponse(
        AuthUserResponse user,
        String sessionToken,
        Instant sessionExpiresAt,
        List<GenerationJobResponse> songs
) {
}
