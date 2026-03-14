package com.balians.musicgen.admin.dto;

import java.time.Instant;
import java.util.List;

public record AdminSongDetailResponse(
        String id,
        String generationJobId,
        String userId,
        String projectId,
        String providerMusicId,
        String title,
        String audioUrl,
        String streamAudioUrl,
        String imageUrl,
        String lyricId,
        String lyricTitle,
        String lyricText,
        String metadataModelName,
        List<String> tags,
        Integer durationSeconds,
        Instant providerCreateTime,
        Instant createdAt
) {
}
