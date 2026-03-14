package com.balians.musicgen.admin.dto;

import java.time.Instant;

public record SecurityLogResponse(
        String id,
        String userId,
        String email,
        String eventType,
        String details,
        Instant occurredAt
) {
}
