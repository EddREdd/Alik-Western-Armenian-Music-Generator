package com.balians.musicgen.media.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.balians.musicgen.common.exception.BadRequestException;
import com.balians.musicgen.media.config.MediaStorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MediaProxyServiceTest {

    private MediaProxyService service;

    @BeforeEach
    void setUp() {
        MediaStorageProperties properties = new MediaStorageProperties();
        properties.setProxyAllowedHosts(java.util.List.of(
                "storage.beesync.co",
                "musicfile.removeai.ai",
                "tempfile.aiquickdraw.com"
        ));
        properties.setSpacesPublicBaseUrl("http://storage.beesync.co:9000/alik");
        service = new MediaProxyService(properties);
    }

    @Test
    void parseAndValidateUrl_allowsBeesyncMinioAudio() {
        var uri = service.parseAndValidateUrl(
                "http://storage.beesync.co:9000/alik/audio/track-1.mp3"
        );

        assertThat(uri.getHost()).isEqualTo("storage.beesync.co");
        assertThat(uri.getPath()).contains("/alik/audio/");
    }

    @Test
    void parseAndValidateUrl_rejectsUnknownHost() {
        assertThatThrownBy(() -> service.parseAndValidateUrl("https://evil.example.com/a.mp3"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void guessContentType_returnsAudioMpegForMp3() {
        var uri = service.parseAndValidateUrl("http://storage.beesync.co:9000/alik/audio/x.mp3");

        assertThat(service.guessContentType(uri)).isEqualTo("audio/mpeg");
    }

    @Test
    void guessContentType_returnsJpegForImagesPath() {
        var uri = service.parseAndValidateUrl("http://storage.beesync.co:9000/alik/images/cover.jpg");

        assertThat(service.guessContentType(uri)).isEqualTo("image/jpeg");
    }
}
