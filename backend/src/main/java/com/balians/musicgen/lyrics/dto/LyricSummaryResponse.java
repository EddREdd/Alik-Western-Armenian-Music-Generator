package com.balians.musicgen.lyrics.dto;

import java.time.Instant;
import java.util.List;

public record LyricSummaryResponse(
        String id,
        String userId,
        String projectId,
        String title,
        String bodyPreview,
        Integer wordCount,
        boolean locked,
        List<String> linkedSongIds,
        Integer currentVersion,
        Instant createdAt,
        Instant updatedAt
) {
}
