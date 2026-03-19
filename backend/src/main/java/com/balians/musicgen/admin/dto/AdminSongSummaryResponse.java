package com.balians.musicgen.admin.dto;

import java.time.Instant;
import java.util.List;

public record AdminSongSummaryResponse(
        String id,
        String generationJobId,
        String userId,
        String userEmail,
        String projectId,
        String title,
        String audioUrl,
        String streamAudioUrl,
        String lyricId,
        String lyricTitle,
        Instant createdAt,
        List<String> tags
) {
}
