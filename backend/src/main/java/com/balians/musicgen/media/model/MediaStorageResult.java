package com.balians.musicgen.media.model;

public record MediaStorageResult(
        String publicUrl,
        String bucket,
        String key,
        String contentType,
        long sizeBytes
) {
}
