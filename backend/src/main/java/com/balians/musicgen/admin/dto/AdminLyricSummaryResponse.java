package com.balians.musicgen.admin.dto;

import java.time.Instant;
import java.util.List;

public record AdminLyricSummaryResponse(
        String id,
        String userId,
        String projectId,
        String title,
        String bodyPreview,
        Integer currentVersion,
        boolean locked,
        List<String> linkedSongIds,
        Instant updatedAt
) {
}
