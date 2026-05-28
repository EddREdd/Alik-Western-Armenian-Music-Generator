package com.balians.musicgen.media.model;

public record RemoteDownloadResult(
        byte[] bytes,
        String contentType
) {
}
