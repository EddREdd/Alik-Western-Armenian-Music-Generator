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
import software.amazon.awssdk.services.s3.model.S3Exception;

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
        } catch (S3Exception ex) {
            String errorCode = ex.awsErrorDetails() == null ? "unknown" : ex.awsErrorDetails().errorCode();
            if ("InvalidAccessKeyId".equals(errorCode)) {
                log.error(
                        "MinIO rejected the configured access key. Check MEDIA_STORAGE_SPACES_ACCESS_KEY / "
                                + "MINIO_ROOT_USER and ensure no old DO_SPACES_* variables are overriding the MinIO values.");
            }
            log.warn(
                    "Failed to store {} asset for track id={} remoteUrl={} because Spaces/S3 rejected the request "
                            + "(statusCode={}, errorCode={}, message={}). Verify MEDIA_STORAGE_SPACES_ACCESS_KEY, "
                            + "MEDIA_STORAGE_SPACES_SECRET_KEY, MEDIA_STORAGE_SPACES_BUCKET, and "
                            + "MEDIA_STORAGE_SPACES_ENDPOINT (S3 API port :9000, not console :9001), or set "
                            + "MEDIA_STORAGE_TYPE=local to disable asset mirroring.",
                    folder,
                    track.getId(),
                    remoteUrl,
                    ex.statusCode(),
                    errorCode,
                    ex.awsErrorDetails() == null ? ex.getMessage() : ex.awsErrorDetails().errorMessage()
            );
            throw new IllegalStateException("Unable to mirror " + folder + " asset to configured Spaces/S3 storage", ex);
        } catch (Exception ex) {
            log.warn("Failed to store {} asset for track id={} remoteUrl={}", folder, track.getId(), remoteUrl, ex);
            throw new IllegalStateException("Unable to mirror " + folder + " asset to configured media storage", ex);
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
        String contentType = resolveContentType(folder);

        try (S3Client s3Client = buildSpacesClient()) {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(mediaStorageProperties.getSpacesBucket())
                    .key(key)
                    .contentType(contentType)
                    .build();
            s3Client.putObject(putRequest, RequestBody.fromBytes(bytes));
        }

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
                || !hasText(mediaStorageProperties.getSpacesSecretKey())) {
            throw new IllegalStateException("DigitalOcean Spaces storage is enabled but configuration is incomplete");
        }
    }

    private S3Client buildSpacesClient() {
        String endpoint = normalizeSpacesEndpoint(mediaStorageProperties.getSpacesEndpoint());
        return S3Client.builder()
                .region(Region.of(mediaStorageProperties.getSpacesRegion()))
                .endpointOverride(URI.create(endpoint))
                .forcePathStyle(true)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                mediaStorageProperties.getSpacesAccessKey(),
                                mediaStorageProperties.getSpacesSecretKey())))
                .build();
    }

    private String resolveContentType(String folder) {
        if ("audio".equals(folder)) {
            return "audio/mpeg";
        }
        if ("images".equals(folder)) {
            return "image/jpeg";
        }
        return "application/octet-stream";
    }

    private String normalizeSpacesEndpoint(String endpoint) {
        if (!hasText(endpoint)) {
            return endpoint;
        }
        return endpoint.trim();
    }

    private String buildSpacesPublicUrl(String key) {
        String configuredBaseUrl = mediaStorageProperties.getSpacesPublicBaseUrl();
        String baseUrl = hasText(configuredBaseUrl)
                ? configuredBaseUrl.trim().replaceAll("/+$", "")
                : normalizeSpacesEndpoint(mediaStorageProperties.getSpacesEndpoint()).replaceAll("/+$", "")
                    + "/" + mediaStorageProperties.getSpacesBucket();
        return baseUrl + "/" + key;
    }
}
