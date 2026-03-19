package com.balians.musicgen.media.controller;

import com.balians.musicgen.common.exception.BadRequestException;
import com.balians.musicgen.media.config.MediaStorageProperties;
import java.io.IOException;
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
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/media")
public class MediaProxyController {

    private final MediaStorageProperties mediaStorageProperties;

    @GetMapping("/proxy")
    public ResponseEntity<byte[]> proxy(@RequestParam("url") String rawUrl) {
        URI uri = parseAndValidate(rawUrl);
        String host = normalizeHost(Objects.requireNonNull(uri.getHost()));
        if (!isAllowedHost(host)) {
            throw new BadRequestException("Media host is not allowed");
        }

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(20))
                .header(HttpHeaders.ACCEPT, "*/*")
                .GET()
                .build();

        try {
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Media proxy fetch failed status={} host={} url={}", response.statusCode(), host, uri);
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
            }

            byte[] body = response.body();
            if (body == null || body.length == 0) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }

            String contentTypeHeader = response.headers()
                    .firstValue(HttpHeaders.CONTENT_TYPE)
                    .orElse("audio/mpeg");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentTypeHeader)
                    .cacheControl(CacheControl.noStore())
                    .body(body);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Media proxy request failed host={} url={}", host, uri, ex);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        } catch (IOException ex) {
            log.warn("Media proxy request failed host={} url={}", host, uri, ex);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    private URI parseAndValidate(String rawUrl) {
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
            return uri;
        } catch (URISyntaxException ex) {
            throw new BadRequestException("Invalid media URL");
        }
    }

    private boolean isAllowedHost(String host) {
        if (!StringUtils.hasText(host)) {
            return false;
        }
        Set<String> allowed = new HashSet<>();
        allowed.addAll(mediaStorageProperties.getProxyAllowedHosts());
        maybeAddHost(allowed, mediaStorageProperties.getPublicBaseUrl());
        maybeAddHost(allowed, mediaStorageProperties.getSpacesPublicBaseUrl());

        return allowed.stream()
                .filter(Objects::nonNull)
                .map(this::normalizeHost)
                .filter(StringUtils::hasText)
                .anyMatch(allowedHost -> host.equals(allowedHost) || host.endsWith("." + allowedHost));
    }

    private void maybeAddHost(Set<String> allowed, String url) {
        if (!StringUtils.hasText(url)) {
            return;
        }
        try {
            URI uri = new URI(url.trim());
            String host = uri.getHost();
            if (StringUtils.hasText(host)) {
                allowed.add(normalizeHost(host));
            }
        } catch (URISyntaxException ignored) {
            // Ignore invalid optional host configs.
        }
    }

    private String normalizeHost(String host) {
        return host.trim().toLowerCase(Locale.ROOT);
    }
}
