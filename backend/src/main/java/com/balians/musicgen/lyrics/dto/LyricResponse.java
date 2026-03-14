package com.balians.musicgen.lyrics.dto;

import java.time.Instant;
import java.util.List;

public record LyricResponse(
        String id,
        String userId,
        String projectId,
        String title,
        String body,
        Integer currentVersion,
        boolean locked,
        Integer wordCount,
        List<String> linkedSongIds,
        List<LyricVersionResponse> versions,
        Instant createdAt,
        Instant updatedAt
) {
}
