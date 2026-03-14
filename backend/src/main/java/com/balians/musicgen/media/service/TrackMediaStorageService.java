package com.balians.musicgen.media.service;

import com.balians.musicgen.generation.model.GenerationTrack;
import com.balians.musicgen.media.config.MediaStorageProperties;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrackMediaStorageService {

    private final MediaStorageProperties mediaStorageProperties;
    private final RestClient.Builder restClientBuilder;

    public void storeTrackAssets(GenerationTrack track) {
        ensureStorageDirectory();
        if (hasText(track.getAudioUrl())) {
            storeAsset(track, track.getAudioUrl(), "audio", ".mp3");
        }
        if (hasText(track.getImageUrl())) {
            storeAsset(track, track.getImageUrl(), "images", ".jpeg");
        }
    }

    private void storeAsset(GenerationTrack track, String remoteUrl, String folder, String defaultExtension) {
        try {
            Path targetPath = buildTargetPath(track, folder, remoteUrl, defaultExtension);
            if (!Files.exists(targetPath)) {
                downloadToPath(remoteUrl, targetPath);
            }
            String publicUrl = buildPublicUrl(folder, targetPath.getFileName().toString());
            if ("audio".equals(folder)) {
                track.setLocalAudioPath(targetPath.toString());
                track.setLocalAudioUrl(publicUrl);
            } else {
                track.setLocalImagePath(targetPath.toString());
                track.setLocalImageUrl(publicUrl);
            }
        } catch (Exception ex) {
            log.warn("Failed to store {} asset for track id={} remoteUrl={}", folder, track.getId(), remoteUrl, ex);
        }
    }

    private void downloadToPath(String remoteUrl, Path targetPath) throws IOException {
        Files.createDirectories(targetPath.getParent());
        RestClient restClient = restClientBuilder.build();
        byte[] bytes = restClient.get()
                .uri(remoteUrl)
                .retrieve()
                .body(byte[].class);
        if (bytes == null || bytes.length == 0) {
            throw new IOException("Downloaded asset is empty");
        }
        Files.write(targetPath, bytes);
    }

    private Path buildTargetPath(GenerationTrack track, String folder, String remoteUrl, String defaultExtension) {
        String providerId = sanitize(track.getProviderMusicId());
        String extension = extractExtension(remoteUrl, defaultExtension);
        String fileName = providerId + extension;
        return Path.of(mediaStorageProperties.getRootPath()).toAbsolutePath().normalize().resolve(folder).resolve(fileName);
    }

    private String extractExtension(String remoteUrl, String defaultExtension) {
        try {
            String path = URI.create(remoteUrl).getPath();
            if (path == null || !path.contains(".")) {
                return defaultExtension;
            }
            String extension = path.substring(path.lastIndexOf('.')).toLowerCase(Locale.ROOT);
            if (extension.length() > 6) {
                return defaultExtension;
            }
            return extension;
        } catch (Exception ex) {
            return defaultExtension;
        }
    }

    private String sanitize(String value) {
        if (!hasText(value)) {
            return "track-" + System.currentTimeMillis();
        }
        return value.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private String buildPublicUrl(String folder, String fileName) {
        String baseUrl = mediaStorageProperties.getPublicBaseUrl().replaceAll("/+$", "");
        return baseUrl + "/media/" + folder + "/" + fileName;
    }

    private void ensureStorageDirectory() {
        try {
            Files.createDirectories(Path.of(mediaStorageProperties.getRootPath()).toAbsolutePath().normalize());
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to create media storage directory", ex);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
