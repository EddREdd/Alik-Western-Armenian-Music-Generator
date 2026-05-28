package com.balians.musicgen.media.service;

import com.balians.musicgen.common.exception.BadRequestException;
import com.balians.musicgen.media.config.MediaStorageProperties;
import com.balians.musicgen.media.model.MediaProxyStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaProxyService {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(120);

    private final MediaStorageProperties mediaStorageProperties;

    public URI parseAndValidateUrl(String rawUrl) {
        if (!StringUtils.hasText(rawUrl)) {
            throw new BadRequestException("url is required");
        }
        try {
            URI uri = new URI(rawUrl.trim());
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
                throw new BadRequestException("Only http/https media URLs are supported");
            }
            if (!StringUtils.hasText(uri.getHost())) {
                throw new BadRequestException("Invalid media URL");
            }
            String host = normalizeHost(uri.getHost());
            if (!isAllowedHost(host)) {
                throw new BadRequestException("Media host is not allowed");
            }
            return uri;
        } catch (URISyntaxException ex) {
            throw new BadRequestException("Invalid media URL");
        }
    }

    public MediaProxyStream openStream(URI uri, String rangeHeader) throws IOException {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(CONNECT_TIMEOUT)
                .build();

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header(HttpHeaders.ACCEPT, "*/*")
                .GET();
        if (StringUtils.hasText(rangeHeader)) {
            requestBuilder.header(HttpHeaders.RANGE, rangeHeader.trim());
        }

        HttpResponse<InputStream> response;
        try {
            response = client.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofInputStream()
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Media proxy request interrupted", ex);
        }

        int statusCode = response.statusCode();
        if (statusCode < 200 || statusCode >= 300) {
            closeQuietly(response.body());
            log.warn("Media proxy upstream failed status={} host={}", statusCode, uri.getHost());
            throw new IOException("Upstream media fetch failed with status " + statusCode);
        }

        String contentType = response.headers()
                .firstValue(HttpHeaders.CONTENT_TYPE)
                .map(value -> value.split(";")[0].trim())
                .orElse(guessContentType(uri));

        Long contentLength = response.headers()
                .firstValue(HttpHeaders.CONTENT_LENGTH)
                .map(this::parseLongHeader)
                .orElse(null);

        String contentRange = response.headers()
                .firstValue(HttpHeaders.CONTENT_RANGE)
                .orElse(null);

        String acceptRanges = response.headers()
                .firstValue(HttpHeaders.ACCEPT_RANGES)
                .orElse("bytes");

        return new MediaProxyStream(
                statusCode,
                response.body(),
                contentType,
                contentLength,
                contentRange,
                acceptRanges
        );
    }

    public boolean isAllowedHost(String host) {
        if (!StringUtils.hasText(host)) {
            return false;
        }
        String normalized = normalizeHost(host);
        Set<String> allowed = new HashSet<>();
        allowed.add("storage.beesync.co");
        allowed.addAll(mediaStorageProperties.getProxyAllowedHosts());
        maybeAddHost(allowed, mediaStorageProperties.getPublicBaseUrl());
        maybeAddHost(allowed, mediaStorageProperties.getSpacesPublicBaseUrl());
        maybeAddHost(allowed, mediaStorageProperties.getSpacesEndpoint());

        return allowed.stream()
                .filter(Objects::nonNull)
                .map(this::normalizeHost)
                .filter(StringUtils::hasText)
                .anyMatch(allowedHost -> normalized.equals(allowedHost) || normalized.endsWith("." + allowedHost));
    }

    String guessContentType(URI uri) {
        String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase(Locale.ROOT);
        if (path.endsWith(".mp3")) {
            return "audio/mpeg";
        }
        if (path.endsWith(".png")) {
            return "image/png";
        }
        if (path.endsWith(".webp")) {
            return "image/webp";
        }
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (path.contains("/images/")) {
            return "image/jpeg";
        }
        return "audio/mpeg";
    }

    private Long parseLongHeader(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void maybeAddHost(Set<String> allowed, String url) {
        if (!StringUtils.hasText(url)) {
            return;
        }
        try {
            URI uri = new URI(url.trim());
            if (StringUtils.hasText(uri.getHost())) {
                allowed.add(normalizeHost(uri.getHost()));
            }
        } catch (URISyntaxException ignored) {
            // Ignore invalid optional host configs.
        }
    }

    private String normalizeHost(String host) {
        return host.trim().toLowerCase(Locale.ROOT);
    }

    private void closeQuietly(InputStream stream) {
        if (stream == null) {
            return;
        }
        try {
            stream.close();
        } catch (IOException ignored) {
            // Ignore close failures on failed upstream responses.
        }
    }
}
