package com.balians.musicgen.media.service;

import static org.assertj.core.api.Assertions.assertThat;
import com.balians.musicgen.generation.model.GenerationTrack;
import com.balians.musicgen.media.config.MediaStorageProperties;
import com.balians.musicgen.media.model.MediaStorageResult;
import com.balians.musicgen.media.model.MediaStorageStatuses;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class TrackMediaStorageServiceTest {

    private MediaStorageProperties properties;
    private TrackMediaStorageService service;

    @BeforeEach
    void setUp() {
        properties = new MediaStorageProperties();
        properties.setType("spaces");
        properties.setSpacesEndpoint("http://storage.beesync.co:9000");
        properties.setSpacesRegion("us-east-1");
        properties.setSpacesBucket("alik");
        properties.setSpacesAccessKey("beesyncadmin");
        properties.setSpacesSecretKey("secret");
        properties.setSpacesPublicBaseUrl("http://storage.beesync.co:9000/alik");
        service = new TrackMediaStorageService(properties, RestClient.builder());
    }

    @Test
    void buildSpacesPublicUrl_usesConfiguredBaseUrl() {
        String publicUrl = service.buildSpacesPublicUrl("audio/track-1-title.mp3");

        assertThat(publicUrl).isEqualTo("http://storage.beesync.co:9000/alik/audio/track-1-title.mp3");
    }

    @Test
    void isConfiguredStorageUrl_detectsBeesyncObjectUrl() {
        assertThat(service.isConfiguredStorageUrl("http://storage.beesync.co:9000/alik/audio/song.mp3"))
                .isTrue();
        assertThat(service.isConfiguredStorageUrl("https://tempfile.aiquickdraw.com/r/song.mp3"))
                .isFalse();
    }

    @Test
    void isAudioAlreadyStored_whenStatusStoredAndKeyPresent() {
        GenerationTrack track = GenerationTrack.builder()
                .mediaStorageStatus(MediaStorageStatuses.STORED)
                .mediaStorageKey("audio/track-1.mp3")
                .build();

        assertThat(service.isAudioAlreadyStored(track)).isTrue();
    }

    @Test
    void storeAudioAsset_withEmptyRemoteUrl_marksSkipped() {
        GenerationTrack track = GenerationTrack.builder().id("track-1").build();

        service.storeAudioAsset(track);

        assertThat(track.getMediaStorageStatus()).isEqualTo(MediaStorageStatuses.SKIPPED);
    }

    @Test
    void storeAudioAsset_whenAlreadyOnMinio_skipsDownload() {
        GenerationTrack track = GenerationTrack.builder()
                .id("track-1")
                .audioUrl("http://storage.beesync.co:9000/alik/audio/existing.mp3")
                .build();

        service.storeAudioAsset(track);

        assertThat(track.getLocalAudioUrl()).isEqualTo("http://storage.beesync.co:9000/alik/audio/existing.mp3");
        assertThat(track.getMediaStorageStatus()).isEqualTo(MediaStorageStatuses.STORED);
    }

    @Test
    void storeAudioAsset_whenDownloadFails_setsFailedStatus() {
        GenerationTrack track = GenerationTrack.builder()
                .id("track-1")
                .audioUrl("http://127.0.0.1:1/not-reachable.mp3")
                .build();

        MediaStorageResult result = service.storeAudioAsset(track, track.getAudioUrl(), "My Song");

        assertThat(result).isNull();
        assertThat(track.getMediaStorageStatus()).isEqualTo(MediaStorageStatuses.FAILED);
        assertThat(track.getMediaStorageError()).isNotBlank();
        assertThat(track.getAudioUrl()).isEqualTo("http://127.0.0.1:1/not-reachable.mp3");
    }
}
