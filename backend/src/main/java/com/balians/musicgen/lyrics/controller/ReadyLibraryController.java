package com.balians.musicgen.lyrics.controller;

import com.balians.musicgen.auth.service.AuthService;
import com.balians.musicgen.common.response.StandardSuccessResponse;
import com.balians.musicgen.lyrics.dto.LyricResponse;
import com.balians.musicgen.lyrics.dto.LyricSummaryResponse;
import com.balians.musicgen.lyrics.service.LyricsService;
import com.balians.musicgen.lyrics.service.ReadyLibraryKeywordValidator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ready-library")
public class ReadyLibraryController {

    private static final String SESSION_HEADER = "X-Session-Token";

    private final LyricsService lyricsService;
    private final AuthService authService;
    private final ReadyLibraryKeywordValidator readyLibraryKeywordValidator;

    @GetMapping
    public StandardSuccessResponse<List<LyricSummaryResponse>> listPublished(
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionToken,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String language
    ) {
        authService.requireAuthenticatedUser(sessionToken);
        readyLibraryKeywordValidator.validate(keyword);
        return StandardSuccessResponse.ok(lyricsService.listReadyLibrary(keyword, language));
    }

    @GetMapping("/{id}")
    public StandardSuccessResponse<LyricResponse> getPublishedById(
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionToken,
            @PathVariable String id
    ) {
        authService.requireAuthenticatedUser(sessionToken);
        return StandardSuccessResponse.ok(lyricsService.getReadyLibraryById(id));
    }
}
