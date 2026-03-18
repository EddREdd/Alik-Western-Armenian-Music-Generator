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
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrackMediaStorageService {

    private final MediaStorageProperties mediaStorageProperties;
    private final RestClient.Builder restClientBuilder;

    public void storeTrackAssets(GenerationTrack track) {
        String audioSourceUrl = hasText(track.getAudioUrl()) ? track.getAudioUrl() : track.getStreamAudioUrl();
        if (hasText(audioSourceUrl)) {
            storeAsset(track, audioSourceUrl, "audio", ".mp3");
        }
        if (hasText(track.getImageUrl())) {
            storeAsset(track, track.getImageUrl(), "images", ".jpeg");
        }
    }

    private void storeAsset(GenerationTrack track, String remoteUrl, String folder, String defaultExtension) {
        try {
            if (isSpacesStorage()) {
                storeAssetToSpaces(track, remoteUrl, folder, defaultExtension);
            } else {
                storeAssetToLocalFilesystem(track, remoteUrl, folder, defaultExtension);
            }
        } catch (Exception ex) {
            log.warn("Failed to store {} asset for track id={} remoteUrl={}", folder, track.getId(), remoteUrl, ex);
        }
    }

    private void downloadToPath(String remoteUrl, Path targetPath) throws IOException {
        Files.createDirectories(targetPath.getParent());
        RestClient restClient = restClientBuilder.build();
        String sourceUrl = remoteUrl == null ? "" : remoteUrl;
        byte[] bytes = restClient.get()
                .uri(sourceUrl)
                .retrieve()
                .body(byte[].class);
        if (bytes == null || bytes.length == 0) {
            throw new IOException("Downloaded asset is empty");
        }
        Files.write(targetPath, bytes);
    }

    private byte[] downloadToBytes(String remoteUrl) throws IOException {
        RestClient restClient = restClientBuilder.build();
        String sourceUrl = remoteUrl == null ? "" : remoteUrl;
        byte[] bytes = restClient.get()
                .uri(sourceUrl)
                .retrieve()
                .body(byte[].class);
        if (bytes == null || bytes.length == 0) {
            throw new IOException("Downloaded asset is empty");
        }
        return bytes;
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

    private boolean isSpacesStorage() {
        return "spaces".equalsIgnoreCase(mediaStorageProperties.getType());
    }

    private void storeAssetToLocalFilesystem(GenerationTrack track, String remoteUrl, String folder,
                                             String defaultExtension) throws IOException {
        ensureStorageDirectory();
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
    }

    private void storeAssetToSpaces(GenerationTrack track, String remoteUrl, String folder,
                                    String defaultExtension) throws IOException {
        validateSpacesConfiguration();
        String providerId = sanitize(track.getProviderMusicId());
        String extension = extractExtension(remoteUrl, defaultExtension);
        String fileName = providerId + extension;
        String key = folder + "/" + fileName;

        byte[] bytes = downloadToBytes(remoteUrl);

        S3Client s3Client = buildSpacesClient();
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(mediaStorageProperties.getSpacesBucket())
                .key(key)
                .build();
        s3Client.putObject(putRequest, RequestBody.fromBytes(bytes));

        String publicUrl = buildSpacesPublicUrl(key);
        if ("audio".equals(folder)) {
            track.setLocalAudioPath(key);
            track.setLocalAudioUrl(publicUrl);
        } else {
            track.setLocalImagePath(key);
            track.setLocalImageUrl(publicUrl);
        }
    }

    private void validateSpacesConfiguration() {
        if (!hasText(mediaStorageProperties.getSpacesEndpoint())
                || !hasText(mediaStorageProperties.getSpacesRegion())
                || !hasText(mediaStorageProperties.getSpacesBucket())
                || !hasText(mediaStorageProperties.getSpacesAccessKey())
                || !hasText(mediaStorageProperties.getSpacesSecretKey())
                || !hasText(mediaStorageProperties.getSpacesPublicBaseUrl())) {
            throw new IllegalStateException("DigitalOcean Spaces storage is enabled but configuration is incomplete");
        }
    }

    private S3Client buildSpacesClient() {
        return S3Client.builder()
                .region(Region.of(mediaStorageProperties.getSpacesRegion()))
                .endpointOverride(URI.create(mediaStorageProperties.getSpacesEndpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                mediaStorageProperties.getSpacesAccessKey(),
                                mediaStorageProperties.getSpacesSecretKey())))
                .build();
    }

    private String buildSpacesPublicUrl(String key) {
        String baseUrl = mediaStorageProperties.getSpacesPublicBaseUrl().replaceAll("/+$", "");
        return baseUrl + "/" + key;
    }
}
