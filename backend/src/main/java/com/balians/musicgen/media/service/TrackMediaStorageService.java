package com.balians.musicgen.media.service;

import com.balians.musicgen.generation.model.GenerationTrack;
import com.balians.musicgen.media.config.MediaStorageProperties;
import com.balians.musicgen.media.model.MediaStorageResult;
import com.balians.musicgen.media.model.MediaStorageStatuses;
import com.balians.musicgen.media.model.RemoteDownloadResult;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrackMediaStorageService {

    private static final Set<String> ALLOWED_AUDIO_CONTENT_TYPES = Set.of(
            "audio/mpeg",
            "audio/mp3",
            "application/octet-stream",
            "audio/x-mpeg",
            "audio/wav",
            "audio/mp4"
    );

    private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp",
            "image/gif",
            "application/octet-stream"
    );

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(120);

    private final MediaStorageProperties mediaStorageProperties;
    private final RestClient.Builder restClientBuilder;

    /**
     * Mirrors provider audio/image assets into configured storage without failing the generation flow.
     */
    public void storeTrackAssets(GenerationTrack track) {
        if (track == null) {
            return;
        }
        storeAudioAsset(track);
        storeImageAsset(track);
    }

    public void storeAudioAsset(GenerationTrack track) {
        if (track == null) {
            return;
        }
        if (isAudioAlreadyStored(track)) {
            log.debug("Skipping audio mirror for trackId={} because asset is already stored", track.getId());
            String existingUrl = firstNonBlank(track.getLocalAudioUrl(), track.getAudioUrl(), track.getStreamAudioUrl());
            if (isConfiguredStorageUrl(existingUrl)) {
                applyExistingStorageUrl(track, existingUrl, "audio");
            }
            return;
        }

        String remoteAudioUrl = resolveRemoteAudioUrl(track);
        if (!hasText(remoteAudioUrl)) {
            markAudioSkipped(track, "No remote audio URL available");
            return;
        }
        if (isConfiguredStorageUrl(remoteAudioUrl)) {
            applyExistingStorageUrl(track, remoteAudioUrl, "audio");
            return;
        }

        preserveProviderAudioUrl(track, remoteAudioUrl);
        storeAudioAsset(track, remoteAudioUrl, track.getTitle());
    }

    public MediaStorageResult storeAudioAsset(GenerationTrack track, String remoteAudioUrl, String titleOrPrompt) {
        String remoteHost = extractHost(remoteAudioUrl);
        log.info("Storing provider audio asset to MinIO for trackId={}, remoteUrlHost={}", track.getId(), remoteHost);
        incrementAudioAttempt(track);

        try {
            RemoteDownloadResult download = downloadRemoteFile(remoteAudioUrl);
            validateAudioContentType(download.contentType());

            String fileName = buildFileName(track, titleOrPrompt, "audio", ".mp3", download.contentType());
            MediaStorageResult result = uploadAsset("audio", fileName, download.bytes(), resolveAudioContentType(download.contentType()));

            applyAudioSuccess(track, remoteAudioUrl, result);
            log.info(
                    "Stored audio asset to MinIO trackId={}, bucket={}, key={}, publicUrl={}",
                    track.getId(),
                    result.bucket(),
                    result.key(),
                    result.publicUrl()
            );
            return result;
        } catch (Exception ex) {
            applyAudioFailure(track, ex);
            log.warn("Failed to store audio asset to MinIO trackId={}, error={}", track.getId(), shortError(ex));
            return null;
        }
    }

    public void storeImageAsset(GenerationTrack track) {
        if (track == null) {
            return;
        }
        if (isImageAlreadyStored(track)) {
            log.debug("Skipping image mirror for trackId={} because image is already stored", track.getId());
            return;
        }

        String remoteImageUrl = resolveRemoteImageUrl(track);
        if (!hasText(remoteImageUrl)) {
            return;
        }
        if (isConfiguredStorageUrl(remoteImageUrl)) {
            applyExistingStorageUrl(track, remoteImageUrl, "images");
            return;
        }

        preserveProviderImageUrl(track, remoteImageUrl);
        storeImageAsset(track, remoteImageUrl, track.getTitle());
    }

    public MediaStorageResult storeImageAsset(GenerationTrack track, String remoteImageUrl, String titleOrPrompt) {
        String remoteHost = extractHost(remoteImageUrl);
        log.info("Storing provider image asset to MinIO for trackId={}, remoteUrlHost={}", track.getId(), remoteHost);

        try {
            RemoteDownloadResult download = downloadRemoteFile(remoteImageUrl);
            validateImageContentType(download.contentType());

            String fileName = buildFileName(track, titleOrPrompt, "images", ".jpg", download.contentType());
            MediaStorageResult result = uploadAsset("images", fileName, download.bytes(), resolveImageContentType(download.contentType()));

            applyImageSuccess(track, remoteImageUrl, result);
            log.info(
                    "Stored image asset to MinIO trackId={}, bucket={}, key={}, publicUrl={}",
                    track.getId(),
                    result.bucket(),
                    result.key(),
                    result.publicUrl()
            );
            return result;
        } catch (Exception ex) {
            applyImageFailure(track, ex);
            log.warn("Failed to store image asset to MinIO trackId={}, error={}", track.getId(), shortError(ex));
            return null;
        }
    }

    public RemoteDownloadResult downloadRemoteFile(String remoteUrl) throws IOException {
        if (!hasText(remoteUrl)) {
            throw new IOException("Remote URL is empty");
        }
        RestClient restClient = restClientBuilder
                .requestFactory(downloadRequestFactory())
                .build();
        var response = restClient.get()
                .uri(remoteUrl.trim())
                .exchange((request, clientResponse) -> {
                    if (!clientResponse.getStatusCode().is2xxSuccessful()) {
                        throw new IOException("Remote download failed with status " + clientResponse.getStatusCode().value());
                    }
                    byte[] bytes = clientResponse.bodyTo(byte[].class);
                    if (bytes == null || bytes.length == 0) {
                        throw new IOException("Downloaded asset is empty");
                    }
                    String contentType = clientResponse.getHeaders().getContentType() == null
                            ? null
                            : clientResponse.getHeaders().getContentType().toString();
                    return new RemoteDownloadResult(bytes, contentType);
                });
        return response;
    }

    public MediaStorageResult uploadToSpaces(String folder, String fileName, byte[] bytes, String contentType) throws IOException {
        if (!isSpacesStorage()) {
            throw new IOException("Spaces/MinIO storage is not enabled");
        }
        validateSpacesConfiguration();
        String key = folder + "/" + fileName;
        uploadToSpaces(key, bytes, contentType);
        return new MediaStorageResult(
                buildSpacesPublicUrl(key),
                mediaStorageProperties.getSpacesBucket(),
                key,
                contentType,
                bytes.length
        );
    }

    boolean isAudioAlreadyStored(GenerationTrack track) {
        if (MediaStorageStatuses.STORED.equalsIgnoreCase(track.getMediaStorageStatus())
                && hasText(track.getMediaStorageKey())) {
            return true;
        }
        return isConfiguredStorageUrl(track.getLocalAudioUrl()) || isConfiguredStorageUrl(track.getAudioUrl());
    }

    boolean isImageAlreadyStored(GenerationTrack track) {
        if (MediaStorageStatuses.STORED.equalsIgnoreCase(track.getImageStorageStatus())
                && hasText(track.getImageStorageKey())) {
            return true;
        }
        return isConfiguredStorageUrl(track.getLocalImageUrl()) || isConfiguredStorageUrl(track.getImageUrl());
    }

    boolean isConfiguredStorageUrl(String url) {
        if (!hasText(url)) {
            return false;
        }
        String normalized = url.trim().toLowerCase(Locale.ROOT);
        String publicBase = mediaStorageProperties.getSpacesPublicBaseUrl();
        if (hasText(publicBase) && normalized.startsWith(publicBase.trim().toLowerCase(Locale.ROOT).replaceAll("/+$", ""))) {
            return true;
        }
        return normalized.contains("storage.beesync.co") && normalized.contains("/alik/");
    }

    String buildSpacesPublicUrl(String key) {
        String configuredBaseUrl = mediaStorageProperties.getSpacesPublicBaseUrl();
        String baseUrl = hasText(configuredBaseUrl)
                ? configuredBaseUrl.trim().replaceAll("/+$", "")
                : normalizeSpacesEndpoint(mediaStorageProperties.getSpacesEndpoint()).replaceAll("/+$", "")
                    + "/" + mediaStorageProperties.getSpacesBucket();
        return baseUrl + "/" + key;
    }

    private MediaStorageResult uploadAsset(String folder, String fileName, byte[] bytes, String contentType) throws IOException {
        if (isSpacesStorage()) {
            return uploadToSpaces(folder, fileName, bytes, contentType);
        }
        return uploadToLocal(folder, fileName, bytes, contentType);
    }

    private MediaStorageResult uploadToLocal(String folder, String fileName, byte[] bytes, String contentType) throws IOException {
        ensureStorageDirectory();
        Path targetPath = Path.of(mediaStorageProperties.getRootPath()).toAbsolutePath().normalize()
                .resolve(folder)
                .resolve(fileName);
        Files.createDirectories(targetPath.getParent());
        Files.write(targetPath, bytes);
        String publicUrl = buildLocalPublicUrl(folder, fileName);
        return new MediaStorageResult(publicUrl, "local", folder + "/" + fileName, contentType, bytes.length);
    }

    private void uploadToSpaces(String key, byte[] bytes, String contentType) {
        try (S3Client s3Client = buildSpacesClient()) {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(mediaStorageProperties.getSpacesBucket())
                    .key(key)
                    .contentType(contentType)
                    .build();
            s3Client.putObject(putRequest, RequestBody.fromBytes(bytes));
        } catch (S3Exception ex) {
            String errorCode = ex.awsErrorDetails() == null ? "unknown" : ex.awsErrorDetails().errorCode();
            if ("InvalidAccessKeyId".equals(errorCode)) {
                log.error(
                        "MinIO rejected the configured access key. Check MEDIA_STORAGE_SPACES_ACCESS_KEY / "
                                + "MINIO_ROOT_USER and ensure no old DO_SPACES_* variables override MinIO values.");
            }
            throw ex;
        }
    }

    private void applyAudioSuccess(GenerationTrack track, String providerUrl, MediaStorageResult result) {
        track.setProviderAudioUrl(providerUrl);
        track.setAudioUrl(result.publicUrl());
        track.setLocalAudioUrl(result.publicUrl());
        track.setStreamAudioUrl(result.publicUrl());
        track.setLocalAudioPath(result.key());
        track.setMediaStorageStatus(MediaStorageStatuses.STORED);
        track.setMediaStorageProvider(MediaStorageStatuses.PROVIDER_MINIO);
        track.setMediaStorageBucket(result.bucket());
        track.setMediaStorageKey(result.key());
        track.setMediaStorageSizeBytes(result.sizeBytes());
        track.setMediaStorageContentType(result.contentType());
        track.setMediaStoredAt(Instant.now());
        track.setMediaStorageError(null);
    }

    private void applyAudioFailure(GenerationTrack track, Exception ex) {
        track.setMediaStorageStatus(MediaStorageStatuses.FAILED);
        track.setMediaStorageError(shortError(ex));
        track.setMediaStorageLastAttemptAt(Instant.now());
    }

    private void applyImageSuccess(GenerationTrack track, String providerUrl, MediaStorageResult result) {
        track.setProviderImageUrl(providerUrl);
        track.setImageUrl(result.publicUrl());
        track.setLocalImageUrl(result.publicUrl());
        track.setLocalImagePath(result.key());
        track.setImageStorageKey(result.key());
        track.setImageStorageStatus(MediaStorageStatuses.STORED);
        track.setImageStorageError(null);
    }

    private void applyImageFailure(GenerationTrack track, Exception ex) {
        track.setImageStorageStatus(MediaStorageStatuses.FAILED);
        track.setImageStorageError(shortError(ex));
    }

    private void markAudioSkipped(GenerationTrack track, String reason) {
        if (!hasText(track.getMediaStorageStatus())) {
            track.setMediaStorageStatus(MediaStorageStatuses.SKIPPED);
            track.setMediaStorageError(reason);
        }
    }

    private void applyExistingStorageUrl(GenerationTrack track, String storageUrl, String folder) {
        if ("audio".equals(folder)) {
            track.setLocalAudioUrl(storageUrl);
            track.setAudioUrl(storageUrl);
            track.setStreamAudioUrl(storageUrl);
            if (!MediaStorageStatuses.STORED.equalsIgnoreCase(track.getMediaStorageStatus())) {
                track.setMediaStorageStatus(MediaStorageStatuses.STORED);
                track.setMediaStorageProvider(MediaStorageStatuses.PROVIDER_MINIO);
            }
        } else {
            track.setLocalImageUrl(storageUrl);
            track.setImageUrl(storageUrl);
            track.setImageStorageStatus(MediaStorageStatuses.STORED);
        }
    }

    private void preserveProviderAudioUrl(GenerationTrack track, String remoteAudioUrl) {
        if (!hasText(track.getProviderAudioUrl()) || isConfiguredStorageUrl(track.getProviderAudioUrl())) {
            track.setProviderAudioUrl(remoteAudioUrl);
        }
    }

    private void preserveProviderImageUrl(GenerationTrack track, String remoteImageUrl) {
        if (!hasText(track.getProviderImageUrl()) || isConfiguredStorageUrl(track.getProviderImageUrl())) {
            track.setProviderImageUrl(remoteImageUrl);
        }
    }

    private String resolveRemoteAudioUrl(GenerationTrack track) {
        if (hasText(track.getProviderAudioUrl()) && !isConfiguredStorageUrl(track.getProviderAudioUrl())) {
            return track.getProviderAudioUrl();
        }
        if (hasText(track.getAudioUrl()) && !isConfiguredStorageUrl(track.getAudioUrl())) {
            return track.getAudioUrl();
        }
        if (hasText(track.getStreamAudioUrl()) && !isConfiguredStorageUrl(track.getStreamAudioUrl())) {
            return track.getStreamAudioUrl();
        }
        return null;
    }

    private String resolveRemoteImageUrl(GenerationTrack track) {
        if (hasText(track.getProviderImageUrl()) && !isConfiguredStorageUrl(track.getProviderImageUrl())) {
            return track.getProviderImageUrl();
        }
        if (hasText(track.getImageUrl()) && !isConfiguredStorageUrl(track.getImageUrl())) {
            return track.getImageUrl();
        }
        return null;
    }

    private void incrementAudioAttempt(GenerationTrack track) {
        int attempts = track.getMediaStorageAttemptCount() == null ? 0 : track.getMediaStorageAttemptCount();
        track.setMediaStorageAttemptCount(attempts + 1);
        track.setMediaStorageLastAttemptAt(Instant.now());
        if (!MediaStorageStatuses.STORED.equalsIgnoreCase(track.getMediaStorageStatus())) {
            track.setMediaStorageStatus(MediaStorageStatuses.PENDING);
        }
    }

    private String buildFileName(
            GenerationTrack track,
            String titleOrPrompt,
            String folder,
            String defaultExtension,
            String contentType
    ) {
        String idPart = hasText(track.getId()) ? sanitize(track.getId()) : sanitize(track.getProviderMusicId());
        String titlePart = sanitize(titleOrPrompt);
        if (titlePart.length() > 48) {
            titlePart = titlePart.substring(0, 48);
        }
        String extension = "audio".equals(folder)
                ? ".mp3"
                : resolveImageExtension(defaultExtension, contentType);
        if (hasText(titlePart) && !"track".equals(titlePart)) {
            return idPart + "-" + titlePart + extension;
        }
        return idPart + extension;
    }

    private String resolveImageExtension(String defaultExtension, String contentType) {
        if (contentType != null) {
            String normalized = contentType.toLowerCase(Locale.ROOT);
            if (normalized.contains("png")) {
                return ".png";
            }
            if (normalized.contains("webp")) {
                return ".webp";
            }
        }
        return ".jpg";
    }

    private void validateAudioContentType(String contentType) throws IOException {
        if (!hasText(contentType)) {
            return;
        }
        String normalized = contentType.toLowerCase(Locale.ROOT).split(";")[0].trim();
        if (normalized.startsWith("audio/") || ALLOWED_AUDIO_CONTENT_TYPES.contains(normalized)) {
            return;
        }
        throw new IOException("Unsupported audio content type: " + contentType);
    }

    private void validateImageContentType(String contentType) throws IOException {
        if (!hasText(contentType)) {
            return;
        }
        String normalized = contentType.toLowerCase(Locale.ROOT).split(";")[0].trim();
        if (normalized.startsWith("image/") || ALLOWED_IMAGE_CONTENT_TYPES.contains(normalized)) {
            return;
        }
        throw new IOException("Unsupported image content type: " + contentType);
    }

    private String resolveAudioContentType(String contentType) {
        if (hasText(contentType)) {
            String normalized = contentType.toLowerCase(Locale.ROOT).split(";")[0].trim();
            if (normalized.startsWith("audio/") || ALLOWED_AUDIO_CONTENT_TYPES.contains(normalized)) {
                return normalized.startsWith("audio/") ? normalized : "audio/mpeg";
            }
        }
        return "audio/mpeg";
    }

    private String resolveImageContentType(String contentType) {
        if (hasText(contentType)) {
            String normalized = contentType.toLowerCase(Locale.ROOT).split(";")[0].trim();
            if (normalized.startsWith("image/") || ALLOWED_IMAGE_CONTENT_TYPES.contains(normalized)) {
                return "image/jpeg";
            }
        }
        return "image/jpeg";
    }

    private SimpleClientHttpRequestFactory downloadRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) CONNECT_TIMEOUT.toMillis());
        factory.setReadTimeout((int) READ_TIMEOUT.toMillis());
        return factory;
    }

    private String buildLocalPublicUrl(String folder, String fileName) {
        String baseUrl = mediaStorageProperties.getPublicBaseUrl().replaceAll("/+$", "");
        return baseUrl + "/media/" + folder + "/" + fileName;
    }

    private void ensureStorageDirectory() throws IOException {
        Files.createDirectories(Path.of(mediaStorageProperties.getRootPath()).toAbsolutePath().normalize());
    }

    private String extractHost(String url) {
        try {
            return URI.create(url.trim()).getHost();
        } catch (Exception ex) {
            return "unknown";
        }
    }

    private String shortError(Exception ex) {
        String message = ex.getMessage();
        if (!hasText(message)) {
            return ex.getClass().getSimpleName();
        }
        return message.length() > 240 ? message.substring(0, 240) : message;
    }

    private String sanitize(String value) {
        if (!hasText(value)) {
            return "track";
        }
        String sanitized = value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]+", "-");
        sanitized = sanitized.replaceAll("-{2,}", "-").replaceAll("^-|-$", "");
        return sanitized.isBlank() ? "track" : sanitized;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private boolean isSpacesStorage() {
        return "spaces".equalsIgnoreCase(mediaStorageProperties.getType());
    }

    private void validateSpacesConfiguration() {
        if (!hasText(mediaStorageProperties.getSpacesEndpoint())
                || !hasText(mediaStorageProperties.getSpacesRegion())
                || !hasText(mediaStorageProperties.getSpacesBucket())
                || !hasText(mediaStorageProperties.getSpacesAccessKey())
                || !hasText(mediaStorageProperties.getSpacesSecretKey())) {
            throw new IllegalStateException("S3-compatible storage is enabled but configuration is incomplete");
        }
        rejectMinioConsolePort(mediaStorageProperties.getSpacesEndpoint());
    }

    private S3Client buildSpacesClient() {
        String endpoint = normalizeSpacesEndpoint(mediaStorageProperties.getSpacesEndpoint());
        return S3Client.builder()
                .region(Region.of(mediaStorageProperties.getSpacesRegion()))
                .endpointOverride(URI.create(endpoint))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                mediaStorageProperties.getSpacesAccessKey(),
                                mediaStorageProperties.getSpacesSecretKey())))
                .build();
    }

    private String normalizeSpacesEndpoint(String endpoint) {
        if (!hasText(endpoint)) {
            return endpoint;
        }
        String trimmed = endpoint.trim();
        if (trimmed.contains(":9001")) {
            throw new IllegalStateException(
                    "MinIO endpoint must use S3 API port :9000, not console port :9001. "
                            + "Use http://storage.beesync.co:9000 (not :9001).");
        }
        return trimmed;
    }

    private void rejectMinioConsolePort(String endpoint) {
        if (hasText(endpoint) && endpoint.contains(":9001")) {
            throw new IllegalStateException(
                    "MinIO endpoint must use S3 API port :9000, not console port :9001. "
                            + "Use http://storage.beesync.co:9000 (not :9001).");
        }
    }
}
