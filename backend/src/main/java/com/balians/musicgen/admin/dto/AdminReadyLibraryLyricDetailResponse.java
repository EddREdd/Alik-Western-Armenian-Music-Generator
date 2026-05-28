package com.balians.musicgen.admin.dto;

import com.balians.musicgen.lyrics.dto.LyricVersionResponse;
import java.time.Instant;
import java.util.List;

public record AdminReadyLibraryLyricDetailResponse(
        String id,
        String projectId,
        String title,
        String body,
        String language,
        Integer currentVersion,
        String createdByAdminUserId,
        String sourceLyricId,
        List<LyricVersionResponse> versions,
        Instant createdAt,
        Instant updatedAt
) {
}
