package com.balians.musicgen.lyrics.controller;

import com.balians.musicgen.auth.service.AuthService;
import com.balians.musicgen.common.response.StandardSuccessResponse;
import com.balians.musicgen.lyrics.dto.CreateLyricRequest;
import com.balians.musicgen.lyrics.dto.LyricResponse;
import com.balians.musicgen.lyrics.dto.LyricSummaryResponse;
import com.balians.musicgen.lyrics.dto.UpdateLyricRequest;
import com.balians.musicgen.lyrics.service.LyricsService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/lyrics")
public class LyricsController {

    private static final String SESSION_HEADER = "X-Session-Token";

    private final LyricsService lyricsService;
    private final AuthService authService;

    @PostMapping
    public StandardSuccessResponse<LyricResponse> create(
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionToken,
            @Valid @RequestBody CreateLyricRequest request
    ) {
        return StandardSuccessResponse.ok(lyricsService.create(request, authService.requireAuthenticatedUser(sessionToken)));
    }

    @GetMapping
    public StandardSuccessResponse<List<LyricSummaryResponse>> listByProject(@RequestParam String projectId) {
        return StandardSuccessResponse.ok(lyricsService.listByProject(projectId));
    }

    @GetMapping("/{id}")
    public StandardSuccessResponse<LyricResponse> getById(@PathVariable String id) {
        return StandardSuccessResponse.ok(lyricsService.getById(id));
    }

    @PutMapping("/{id}")
    public StandardSuccessResponse<LyricResponse> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateLyricRequest request
    ) {
        return StandardSuccessResponse.ok(lyricsService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public StandardSuccessResponse<String> delete(@PathVariable String id) {
        lyricsService.delete(id);
        return StandardSuccessResponse.ok("deleted");
    }
}
