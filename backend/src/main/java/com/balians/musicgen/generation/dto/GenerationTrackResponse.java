package com.balians.musicgen.generation.dto;

import java.time.Instant;
import java.util.List;

public record GenerationTrackResponse(
        String id,
        String providerMusicId,
        Integer trackIndex,
        String audioUrl,
        String streamAudioUrl,
        String imageUrl,
        String localAudioUrl,
        String localImageUrl,
        String lyricsOrPrompt,
        String title,
        List<String> tags,
        Integer durationSeconds,
        Instant providerCreateTime,
        Instant assetExpiryAt,
        Boolean selectedFlag,
        Instant createdAt
) {
}
