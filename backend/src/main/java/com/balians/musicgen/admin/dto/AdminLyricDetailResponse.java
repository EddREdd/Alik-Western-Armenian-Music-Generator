package com.balians.musicgen.admin.dto;

import java.time.Instant;
import java.util.List;

public record AdminLyricDetailResponse(
        String id,
        String userId,
        String projectId,
        String title,
        String body,
        Integer currentVersion,
        boolean locked,
        List<String> linkedSongIds,
        Instant createdAt,
        Instant updatedAt
) {
}
