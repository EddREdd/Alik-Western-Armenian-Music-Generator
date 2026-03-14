package com.balians.musicgen.lyrics.dto;

import java.time.Instant;

public record LyricVersionResponse(
        Integer versionNumber,
        String title,
        String body,
        Instant editedAt
) {
}
