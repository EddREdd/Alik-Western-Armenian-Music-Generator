package com.balians.musicgen.admin.dto;

import java.time.Instant;

public record AdminReadyLibraryLyricSummaryResponse(
        String id,
        String title,
        String bodyPreview,
        String language,
        Integer currentVersion,
        String createdByAdminUserId,
        Instant createdAt,
        Instant updatedAt
) {
}
