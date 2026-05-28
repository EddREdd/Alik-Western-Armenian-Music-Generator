package com.balians.musicgen.media.controller;

import com.balians.musicgen.media.model.MediaProxyStream;
import com.balians.musicgen.media.service.MediaProxyService;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/media")
public class MediaProxyController {

    private final MediaProxyService mediaProxyService;

    @GetMapping("/proxy")
    public ResponseEntity<StreamingResponseBody> proxy(
            @RequestParam("url") String rawUrl,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader
    ) {
        URI uri = mediaProxyService.parseAndValidateUrl(rawUrl);

        MediaProxyStream upstream;
        try {
            upstream = mediaProxyService.openStream(uri, rangeHeader);
        } catch (IOException ex) {
            log.warn("Media proxy failed host={} message={}", uri.getHost(), ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }

        StreamingResponseBody body = outputStream -> {
            try (InputStream input = upstream.body()) {
                input.transferTo(outputStream);
            }
        };

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, upstream.contentType());
        headers.set(HttpHeaders.ACCEPT_RANGES, upstream.acceptRanges());
        headers.setCacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic());
        if (upstream.contentLength() != null && upstream.contentLength() > 0) {
            headers.setContentLength(upstream.contentLength());
        }
        if (upstream.contentRange() != null) {
            headers.set(HttpHeaders.CONTENT_RANGE, upstream.contentRange());
        }

        return new ResponseEntity<>(body, headers, HttpStatus.valueOf(upstream.statusCode()));
    }
}
