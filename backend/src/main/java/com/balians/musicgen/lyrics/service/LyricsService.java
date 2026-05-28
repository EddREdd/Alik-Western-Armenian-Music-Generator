package com.balians.musicgen.lyrics.service;

import com.balians.musicgen.auth.model.UserAccount;
import com.balians.musicgen.common.exception.BadRequestException;
import com.balians.musicgen.common.exception.ConflictException;
import com.balians.musicgen.common.exception.NotFoundException;
import com.balians.musicgen.lyrics.dto.CreateLyricRequest;
import com.balians.musicgen.lyrics.dto.LyricResponse;
import com.balians.musicgen.lyrics.dto.LyricSummaryResponse;
import com.balians.musicgen.lyrics.dto.LyricVersionResponse;
import com.balians.musicgen.lyrics.dto.UpdateLyricRequest;
import com.balians.musicgen.lyrics.model.LyricEntry;
import com.balians.musicgen.lyrics.model.LyricLanguage;
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
    private final LyricTextValidator lyricTextValidator;
    private final ReadyLibraryService readyLibraryService;

    public String autoProjectId(UserAccount owner) {
        if (owner != null && hasText(owner.getId())) {
            return "user-" + owner.getId().trim();
        }
        return "system";
    }

    public String resolveProjectId(String projectId, UserAccount owner) {
        if (hasText(projectId)) {
            return projectId.trim();
        }
        return autoProjectId(owner);
    }

    public List<LyricSummaryResponse> listReadyLibrary(String keyword, String language) {
        return readyLibraryService.listPublic(keyword, language);
    }

    public LyricResponse getReadyLibraryById(String id) {
        return readyLibraryService.getPublicById(id);
    }

    public LyricResponse create(CreateLyricRequest request, UserAccount owner) {
        requireOwner(owner);
        LyricLanguage language = request.language() == null || request.language().isBlank()
                ? LyricLanguage.WESTERN_ARMENIAN
                : LyricLanguage.parseRequired(request.language(), "language");
        lyricTextValidator.validateTitle(request.title(), language);
        lyricTextValidator.validateBody(request.body(), language);

        String projectId = resolveProjectId(request.projectId(), owner);
        LyricEntry entry = lyricEntryRepository.save(LyricEntry.builder()
                .userId(owner.getId())
                .projectId(projectId)
                .title(request.title().trim())
                .body(normalizeBody(request.body()))
                .language(language.name())
                .currentVersion(1)
                .locked(false)
                .publicReadyLibrary(false)
                .build());

        log.info("Created lyric entry id={} projectId={} userId={}", entry.getId(), entry.getProjectId(), entry.getUserId());
        return toResponse(entry);
    }

    public List<LyricSummaryResponse> listForUser(UserAccount owner, String projectId) {
        requireOwner(owner);
        String ownerUserId = owner.getId().trim();
        List<LyricEntry> entries;
        if (hasText(projectId)) {
            entries = lyricEntryRepository.findPrivateByUserIdAndProjectIdOrderByUpdatedAtDesc(
                    ownerUserId,
                    projectId.trim()
            );
        } else {
            entries = lyricEntryRepository.findPrivateByUserIdOrderByUpdatedAtDesc(ownerUserId);
        }
        return entries.stream().map(this::toSummary).toList();
    }

    public LyricResponse getById(String id, UserAccount owner) {
        requireOwner(owner);
        return toResponse(getPrivateEntityForOwner(id, owner.getId()));
    }

    public LyricResponse update(String id, UpdateLyricRequest request, UserAccount owner) {
        requireOwner(owner);
        LyricEntry entry = getPrivateEntityForOwner(id, owner.getId());
        ensureEditable(entry);
        LyricLanguage language = resolveEntryLanguage(entry);
        lyricTextValidator.validateTitle(request.title(), language);
        lyricTextValidator.validateBody(request.body(), language);

        appendVersionSnapshot(entry);
        entry.setCurrentVersion(entry.getCurrentVersion() + 1);
        entry.setTitle(request.title().trim());
        entry.setBody(normalizeBody(request.body()));

        LyricEntry saved = lyricEntryRepository.save(entry);
        log.info("Updated lyric entry id={} version={}", saved.getId(), saved.getCurrentVersion());
        return toResponse(saved);
    }

    public LyricResponse restoreVersion(String id, Integer versionNumber, UserAccount owner) {
        requireOwner(owner);
        if (versionNumber == null) {
            throw new BadRequestException("versionNumber is required");
        }

        LyricEntry entry = getPrivateEntityForOwner(id, owner.getId());
        LyricVersion selected = entry.getVersions().stream()
                .filter(version -> versionNumber.equals(version.getVersionNumber()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Lyric version not found: " + versionNumber));

        if (Boolean.TRUE.equals(entry.getLocked())) {
            String originalProjectId = hasText(entry.getProjectId()) ? entry.getProjectId() : autoProjectId(owner);
            LyricEntry restoredCopy = lyricEntryRepository.save(LyricEntry.builder()
                    .userId(owner.getId())
                    .projectId(originalProjectId)
                    .title(selected.getTitle() + " (Restored v" + versionNumber + ")")
                    .body(normalizeBody(selected.getBody()))
                    .language(entry.getLanguage() == null ? ReadyLibraryConstants.DEFAULT_LANGUAGE : entry.getLanguage())
                    .sourceLyricId(entry.getId())
                    .publicReadyLibrary(false)
                    .createdByAdminUserId(null)
                    .currentVersion(1)
                    .locked(false)
                    .build());
            return toResponse(restoredCopy);
        }

        appendVersionSnapshot(entry);
        entry.setCurrentVersion(entry.getCurrentVersion() + 1);
        entry.setTitle(selected.getTitle());
        entry.setBody(normalizeBody(selected.getBody()));
        LyricEntry saved = lyricEntryRepository.save(entry);
        return toResponse(saved);
    }

    public void delete(String id, UserAccount owner) {
        requireOwner(owner);
        LyricEntry entry = getPrivateEntityForOwner(id, owner.getId());
        ensureEditable(entry);
        lyricEntryRepository.delete(entry);
        log.info("Deleted lyric entry id={} userId={}", id, owner.getId());
    }

    public LyricResponse linkToSong(String lyricId, String generationJobId) {
        LyricEntry entry = getEntity(lyricId);
        if (Boolean.TRUE.equals(entry.getPublicReadyLibrary())) {
            throw new BadRequestException("Public Ready Library lyrics cannot be linked to generation jobs");
        }
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
        if (Boolean.TRUE.equals(entry.getPublicReadyLibrary())) {
            return;
        }
        List<String> linkedSongIds = new ArrayList<>(entry.getLinkedSongIds());
        linkedSongIds.removeIf(songId -> songId.equals(generationJobId));
        entry.setLinkedSongIds(linkedSongIds);
        entry.setLocked(!linkedSongIds.isEmpty());
        lyricEntryRepository.save(entry);
    }

    public LyricResponse assertAvailableForGeneration(String lyricId, UserAccount owner) {
        requireOwner(owner);
        return toResponse(getPrivateEntityForOwner(lyricId, owner.getId()));
    }

    private LyricEntry getPrivateEntityForOwner(String id, String userId) {
        return lyricEntryRepository.findByIdAndUserId(id, userId)
                .filter(this::isPrivateLyric)
                .orElseThrow(() -> new NotFoundException("Lyric not found: " + id));
    }

    private LyricEntry getEntity(String id) {
        return lyricEntryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Lyric not found: " + id));
    }

    private boolean isPrivateLyric(LyricEntry entry) {
        return !Boolean.TRUE.equals(entry.getPublicReadyLibrary());
    }

    private LyricLanguage resolveEntryLanguage(LyricEntry entry) {
        if (entry.getLanguage() == null || entry.getLanguage().isBlank()) {
            return LyricLanguage.WESTERN_ARMENIAN;
        }
        return LyricLanguage.parseRequired(entry.getLanguage(), "language");
    }

    private void requireOwner(UserAccount owner) {
        if (owner == null || !hasText(owner.getId())) {
            throw new BadRequestException("Authentication is required");
        }
    }

    private void ensureEditable(LyricEntry entry) {
        if (Boolean.TRUE.equals(entry.getLocked()) || !entry.getLinkedSongIds().isEmpty()) {
            throw new ConflictException("Lyric is linked to generated songs and is read-only");
        }
    }

    private void appendVersionSnapshot(LyricEntry entry) {
        List<LyricVersion> versions = new ArrayList<>(entry.getVersions());
        versions.add(LyricVersion.builder()
                .versionNumber(entry.getCurrentVersion())
                .title(entry.getTitle())
                .body(entry.getBody())
                .editedAt(Instant.now())
                .build());
        entry.setVersions(versions);
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
                entry.getLanguage() == null ? ReadyLibraryConstants.DEFAULT_LANGUAGE : entry.getLanguage(),
                Boolean.TRUE.equals(entry.getPublicReadyLibrary()),
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
                entry.getLanguage() == null ? ReadyLibraryConstants.DEFAULT_LANGUAGE : entry.getLanguage(),
                Boolean.TRUE.equals(entry.getPublicReadyLibrary()),
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

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
