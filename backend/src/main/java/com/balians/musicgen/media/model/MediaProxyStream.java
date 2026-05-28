package com.balians.musicgen.media.model;

import java.io.InputStream;

public record MediaProxyStream(
        int statusCode,
        InputStream body,
        String contentType,
        Long contentLength,
        String contentRange,
        String acceptRanges
) {
}
