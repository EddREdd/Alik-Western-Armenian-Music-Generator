package com.balians.musicgen.admin.dto;

import java.time.Instant;

public record AdminInviteCodeResponse(
        String id,
        String code,
        boolean active,
        String usedByUserId,
        String usedByEmail,
        Instant usedAt,
        Instant createdAt
) {
}
