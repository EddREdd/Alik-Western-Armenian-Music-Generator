package com.balians.musicgen.lyrics.service;

import com.balians.musicgen.common.exception.BadRequestException;
import com.balians.musicgen.common.exception.ConflictException;
import com.balians.musicgen.common.exception.NotFoundException;
import com.balians.musicgen.auth.model.UserAccount;
import com.balians.musicgen.lyrics.dto.CreateLyricRequest;
import com.balians.musicgen.lyrics.dto.LyricResponse;
import com.balians.musicgen.lyrics.dto.LyricSummaryResponse;
import com.balians.musicgen.lyrics.dto.LyricVersionResponse;
import com.balians.musicgen.lyrics.dto.UpdateLyricRequest;
import com.balians.musicgen.lyrics.model.LyricEntry;
import com.balians.musicgen.lyrics.model.LyricVersion;
import com.balians.musicgen.lyrics.repository.LyricEntryRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LyricsService {

    private final LyricEntryRepository lyricEntryRepository;

    public LyricResponse create(CreateLyricRequest request, UserAccount owner) {
        validateArmenianText(request.title(), "title");
        validateArmenianText(request.body(), "body");

        LyricEntry entry = lyricEntryRepository.save(LyricEntry.builder()
                .userId(owner == null ? null : owner.getId())
                .projectId(request.projectId().trim())
                .title(request.title().trim())
                .body(normalizeBody(request.body()))
                .currentVersion(1)
                .locked(false)
                .build());

        log.info("Created lyric entry id={} projectId={}", entry.getId(), entry.getProjectId());
        return toResponse(entry);
    }

    public List<LyricSummaryResponse> listByProject(String projectId) {
        return lyricEntryRepository.findByProjectIdOrderByUpdatedAtDesc(projectId.trim())
                .stream()
                .map(this::toSummary)
                .toList();
    }

    public LyricResponse getById(String id) {
        return toResponse(getEntity(id));
    }

    public LyricResponse update(String id, UpdateLyricRequest request) {
        LyricEntry entry = getEntity(id);
        ensureEditable(entry);
        validateArmenianText(request.title(), "title");
        validateArmenianText(request.body(), "body");

        List<LyricVersion> versions = new ArrayList<>(entry.getVersions());
        versions.add(LyricVersion.builder()
                .versionNumber(entry.getCurrentVersion())
                .title(entry.getTitle())
                .body(entry.getBody())
                .editedAt(Instant.now())
                .build());

        entry.setVersions(versions);
        entry.setCurrentVersion(entry.getCurrentVersion() + 1);
        entry.setTitle(request.title().trim());
        entry.setBody(normalizeBody(request.body()));

        LyricEntry saved = lyricEntryRepository.save(entry);
        log.info("Updated lyric entry id={} version={}", saved.getId(), saved.getCurrentVersion());
        return toResponse(saved);
    }

    public void delete(String id) {
        LyricEntry entry = getEntity(id);
        ensureEditable(entry);
        lyricEntryRepository.delete(entry);
        log.info("Deleted lyric entry id={}", id);
    }

    public LyricResponse linkToSong(String lyricId, String generationJobId) {
        LyricEntry entry = getEntity(lyricId);
        List<String> linkedSongIds = new ArrayList<>(entry.getLinkedSongIds());
        if (!linkedSongIds.contains(generationJobId)) {
            linkedSongIds.add(generationJobId);
        }
        entry.setLinkedSongIds(linkedSongIds);
        entry.setLocked(!linkedSongIds.isEmpty());
        return toResponse(lyricEntryRepository.save(entry));
    }

    public void unlinkFromSong(String lyricId, String generationJobId) {
        LyricEntry entry = getEntity(lyricId);
        List<String> linkedSongIds = new ArrayList<>(entry.getLinkedSongIds());
        linkedSongIds.removeIf(songId -> songId.equals(generationJobId));
        entry.setLinkedSongIds(linkedSongIds);
        entry.setLocked(!linkedSongIds.isEmpty());
        lyricEntryRepository.save(entry);
    }

    public LyricResponse assertAvailableForGeneration(String lyricId) {
        LyricEntry entry = getEntity(lyricId);
        return toResponse(entry);
    }

    private LyricEntry getEntity(String id) {
        return lyricEntryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Lyric not found: " + id));
    }

    private void ensureEditable(LyricEntry entry) {
        if (Boolean.TRUE.equals(entry.getLocked()) || !entry.getLinkedSongIds().isEmpty()) {
            throw new ConflictException("Lyric is linked to generated songs and is read-only");
        }
    }

    private void validateArmenianText(String value, String fieldName) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isBlank()) {
            throw new BadRequestException(fieldName + " is required");
        }

        boolean hasArmenianLetter = false;
        for (int i = 0; i < trimmed.length(); i++) {
            char current = trimmed.charAt(i);
            if (Character.isLetter(current)) {
                Character.UnicodeBlock block = Character.UnicodeBlock.of(current);
                if (block != Character.UnicodeBlock.ARMENIAN) {
                    throw new BadRequestException(fieldName + " must use the Armenian alphabet only");
                }
                hasArmenianLetter = true;
            }
        }

        if (!hasArmenianLetter) {
            throw new BadRequestException(fieldName + " must contain Armenian letters");
        }
    }

    private String normalizeBody(String body) {
        return body.trim().replace("\r\n", "\n");
    }

    private LyricResponse toResponse(LyricEntry entry) {
        return new LyricResponse(
                entry.getId(),
                entry.getUserId(),
                entry.getProjectId(),
                entry.getTitle(),
                entry.getBody(),
                entry.getCurrentVersion(),
                Boolean.TRUE.equals(entry.getLocked()),
                countWords(entry.getBody()),
                List.copyOf(entry.getLinkedSongIds()),
                entry.getVersions().stream()
                        .map(version -> new LyricVersionResponse(
                                version.getVersionNumber(),
                                version.getTitle(),
                                version.getBody(),
                                version.getEditedAt()
                        ))
                        .toList(),
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }

    private LyricSummaryResponse toSummary(LyricEntry entry) {
        String body = entry.getBody() == null ? "" : entry.getBody();
        String preview = body.length() > 140 ? body.substring(0, 140) + "..." : body;
        return new LyricSummaryResponse(
                entry.getId(),
                entry.getUserId(),
                entry.getProjectId(),
                entry.getTitle(),
                preview,
                countWords(body),
                Boolean.TRUE.equals(entry.getLocked()),
                List.copyOf(entry.getLinkedSongIds()),
                entry.getCurrentVersion(),
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }

    private int countWords(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        return (int) List.of(value.trim().split("\\s+")).stream()
                .filter(word -> !word.isBlank())
                .count();
    }
}
