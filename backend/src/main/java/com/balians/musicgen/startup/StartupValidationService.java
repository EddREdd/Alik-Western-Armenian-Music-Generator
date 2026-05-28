package com.balians.musicgen.startup;

import com.balians.musicgen.config.FeatureFlagsProperties;
import com.balians.musicgen.media.config.MediaStorageProperties;
import com.balians.musicgen.provider.config.ProviderProperties;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StartupValidationService {

    private final FeatureFlagsProperties featureFlagsProperties;
    private final ProviderProperties providerProperties;
    private final MediaStorageProperties mediaStorageProperties;
    private final MongoProperties mongoProperties;

    @PostConstruct
    public void validate() {
        requireText(mongoProperties.getUri(), "spring.data.mongodb.uri is required");
        rejectLocalMongoUri(mongoProperties.getUri());

        if (featureFlagsProperties.isProviderSubmissionEnabled()) {
            requireText(providerProperties.getBaseUrl(), "provider.base-url is required when provider submission is enabled");
            requireText(providerProperties.getApiKey(), "provider.api-key is required when provider submission is enabled");
            requireText(providerProperties.getCallbackBaseUrl(), "provider.callback-base-url is required when provider submission is enabled");
        }

        validateMediaStorageConfiguration();

        log.info("Startup configuration validated: providerSubmissionEnabled={}, callbackProcessingEnabled={}, adminEndpointsEnabled={}",
                featureFlagsProperties.isProviderSubmissionEnabled(),
                featureFlagsProperties.isCallbackProcessingEnabled(),
                featureFlagsProperties.isAdminEndpointsEnabled());
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank() || "change-me".equalsIgnoreCase(value.trim())) {
            throw new IllegalStateException(message);
        }
    }

    private void validateMediaStorageConfiguration() {
        if (!"spaces".equalsIgnoreCase(mediaStorageProperties.getType())) {
            return;
        }

        requireText(mediaStorageProperties.getSpacesEndpoint(),
                "media.storage.spaces-endpoint is required when media.storage.type=spaces");
        requireText(mediaStorageProperties.getSpacesRegion(),
                "media.storage.spaces-region is required when media.storage.type=spaces");
        requireText(mediaStorageProperties.getSpacesBucket(),
                "media.storage.spaces-bucket is required when media.storage.type=spaces");
        requireText(mediaStorageProperties.getSpacesAccessKey(),
                "media.storage.spaces-access-key is required when media.storage.type=spaces");
        requireText(mediaStorageProperties.getSpacesSecretKey(),
                "media.storage.spaces-secret-key is required when media.storage.type=spaces");
        requireText(mediaStorageProperties.getSpacesPublicBaseUrl(),
                "media.storage.spaces-public-base-url is required when media.storage.type=spaces");

        rejectMinioConsolePort(mediaStorageProperties.getSpacesEndpoint(),
                "media.storage.spaces-endpoint must use MinIO S3 API port :9000, not console port :9001");
        rejectMinioConsolePort(mediaStorageProperties.getSpacesPublicBaseUrl(),
                "media.storage.spaces-public-base-url must not use MinIO console port :9001");

        log.info(
                "Media storage resolved config: type={}, endpoint={}, region={}, bucket={}, publicBaseUrl={}, accessKeyPrefix={}",
                mediaStorageProperties.getType(),
                mediaStorageProperties.getSpacesEndpoint(),
                mediaStorageProperties.getSpacesRegion(),
                mediaStorageProperties.getSpacesBucket(),
                mediaStorageProperties.getSpacesPublicBaseUrl(),
                accessKeyPrefix(mediaStorageProperties.getSpacesAccessKey()));

        warnIfMinioNotPubliclyReadable();
    }

    private void warnIfMinioNotPubliclyReadable() {
        String probeUrl = mediaStorageProperties.getPublicProbeUrl();
        if (probeUrl == null || probeUrl.isBlank()) {
            log.info(
                    "MinIO public-read probe skipped. Set MEDIA_STORAGE_PUBLIC_PROBE_URL to verify anonymous download, "
                            + "or configure: mc anonymous set download beesync/alik/audio");
            return;
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(probeUrl.trim()))
                    .timeout(Duration.ofSeconds(15))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            int status = response.statusCode();
            if (status == 401 || status == 403) {
                log.warn(
                        "MinIO object storage is writable but not publicly readable. Configure "
                                + "'mc anonymous set download' for bucket/prefix or enable backend media proxy for playback. "
                                + "probeUrl={} status={}",
                        probeUrl,
                        status
                );
            } else if (status >= 200 && status < 300) {
                log.info("MinIO public-read probe succeeded status={} probeUrl={}", status, probeUrl);
            } else {
                log.warn("MinIO public-read probe returned unexpected status={} probeUrl={}", status, probeUrl);
            }
        } catch (Exception ex) {
            log.warn(
                    "MinIO public-read probe failed (non-fatal). Configure mc anonymous set download for "
                            + "beesync/alik/audio and beesync/alik/images, or use /api/v1/media/proxy. probeUrl={} error={}",
                    probeUrl,
                    ex.getMessage()
            );
        }
    }

    private void rejectMinioConsolePort(String value, String message) {
        if (value != null && value.contains(":9001")) {
            throw new IllegalStateException(message);
        }
    }

    private String accessKeyPrefix(String accessKey) {
        if (accessKey == null || accessKey.isBlank()) {
            return "(empty)";
        }
        String trimmed = accessKey.trim();
        int prefixLength = Math.min(3, trimmed.length());
        return trimmed.substring(0, prefixLength) + "***";
    }

    private void rejectLocalMongoUri(String uri) {
        String normalized = uri.trim().toLowerCase();
        if (normalized.contains("localhost") || normalized.contains("127.0.0.1")) {
            throw new IllegalStateException("spring.data.mongodb.uri must point to the server database, not localhost");
        }
    }
}
