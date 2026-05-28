package com.balians.musicgen.lyrics.service;

import com.balians.musicgen.admin.dto.AdminReadyLibraryLyricDetailResponse;
import com.balians.musicgen.admin.dto.AdminReadyLibraryLyricSummaryResponse;
import com.balians.musicgen.admin.dto.CreateReadyLibraryLyricRequest;
import com.balians.musicgen.admin.dto.SetReadyLibraryPublishedRequest;
import com.balians.musicgen.admin.dto.UpdateReadyLibraryLyricRequest;
import com.balians.musicgen.common.exception.NotFoundException;
import com.balians.musicgen.lyrics.dto.LyricResponse;
import com.balians.musicgen.lyrics.dto.LyricSummaryResponse;
import com.balians.musicgen.lyrics.dto.LyricVersionResponse;
import com.balians.musicgen.lyrics.model.LyricEntry;
import com.balians.musicgen.lyrics.model.LyricLanguage;
import com.balians.musicgen.lyrics.model.LyricVersion;
import com.balians.musicgen.lyrics.repository.LyricEntryRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReadyLibraryService {

    private final LyricEntryRepository lyricEntryRepository;
    private final LyricTextValidator lyricTextValidator;

    public List<LyricSummaryResponse> listPublic(String keyword, String language) {
        LyricLanguage languageFilter = LyricLanguage.parseOptional(language);
        List<LyricEntry> entries = languageFilter == null
                ? lyricEntryRepository.findByPublicReadyLibraryTrueOrderByUpdatedAtDesc()
                : lyricEntryRepository.findByPublicReadyLibraryTrueAndLanguageOrderByUpdatedAtDesc(languageFilter.name());

        return entries.stream()
                .filter(entry -> isPublished(entry))
                .filter(entry -> matchesKeyword(entry, keyword))
                .map(this::toPublicSummary)
                .toList();
    }

    public LyricResponse getPublicById(String id) {
        LyricEntry entry = getReadyLibraryEntity(id);
        if (!isPublished(entry)) {
            throw new NotFoundException("Lyric not found: " + id);
        }
        return toPublicResponse(entry);
    }

    public List<AdminReadyLibraryLyricSummaryResponse> listForAdmin(String keyword, String language) {
        LyricLanguage languageFilter = LyricLanguage.parseOptional(language);
        List<LyricEntry> entries = languageFilter == null
                ? lyricEntryRepository.findByPublicReadyLibraryTrueOrderByUpdatedAtDesc()
                : lyricEntryRepository.findByPublicReadyLibraryTrueAndLanguageOrderByUpdatedAtDesc(languageFilter.name());

        return entries.stream()
                .filter(entry -> matchesKeyword(entry, keyword))
                .map(this::toAdminSummary)
                .toList();
    }

    public AdminReadyLibraryLyricDetailResponse getForAdmin(String id) {
        return toAdminDetail(getReadyLibraryEntity(id));
    }

    public AdminReadyLibraryLyricDetailResponse create(String adminUserId, CreateReadyLibraryLyricRequest request) {
        LyricLanguage language = LyricLanguage.parseRequired(request.language(), "language");
        lyricTextValidator.validateTitle(request.title(), language);
        lyricTextValidator.validateBody(request.body(), language);

        LyricEntry entry = lyricEntryRepository.save(LyricEntry.builder()
                .userId(null)
                .projectId(ReadyLibraryConstants.PROJECT_ID)
                .title(request.title().trim())
                .body(normalizeBody(request.body()))
                .language(language.name())
                .publicReadyLibrary(true)
                .readyLibraryPublished(resolvePublishedOnCreate(request.published()))
                .createdByAdminUserId(adminUserId)
                .sourceLyricId(null)
                .currentVersion(1)
                .locked(false)
                .build());

        log.info("Created Ready Library lyric id={} language={} adminUserId={}", entry.getId(), entry.getLanguage(), adminUserId);
        return toAdminDetail(entry);
    }

    public AdminReadyLibraryLyricDetailResponse update(String id, UpdateReadyLibraryLyricRequest request) {
        LyricEntry entry = getReadyLibraryEntity(id);
        LyricLanguage language = LyricLanguage.parseRequired(request.language(), "language");
        lyricTextValidator.validateTitle(request.title(), language);
        lyricTextValidator.validateBody(request.body(), language);

        appendVersionSnapshot(entry);

        entry.setCurrentVersion(entry.getCurrentVersion() + 1);
        entry.setTitle(request.title().trim());
        entry.setBody(normalizeBody(request.body()));
        entry.setLanguage(language.name());

        LyricEntry saved = lyricEntryRepository.save(entry);
        log.info("Updated Ready Library lyric id={} version={}", saved.getId(), saved.getCurrentVersion());
        return toAdminDetail(saved);
    }

    public void delete(String id) {
        LyricEntry entry = getReadyLibraryEntity(id);
        lyricEntryRepository.delete(entry);
        log.info("Deleted Ready Library lyric id={}", id);
    }

    public AdminReadyLibraryLyricDetailResponse setPublished(String id, SetReadyLibraryPublishedRequest request) {
        LyricEntry entry = getReadyLibraryEntity(id);
        entry.setReadyLibraryPublished(Boolean.TRUE.equals(request.published()));
        LyricEntry saved = lyricEntryRepository.save(entry);
        log.info("Set Ready Library lyric id={} published={}", id, saved.getReadyLibraryPublished());
        return toAdminDetail(saved);
    }

    public boolean isReadyLibraryLyric(String lyricId) {
        return lyricEntryRepository.findById(lyricId)
                .map(entry -> Boolean.TRUE.equals(entry.getPublicReadyLibrary()))
                .orElse(false);
    }

    private LyricEntry getReadyLibraryEntity(String id) {
        return lyricEntryRepository.findByIdAndPublicReadyLibraryTrue(id)
                .orElseThrow(() -> new NotFoundException("Lyric not found: " + id));
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

    private boolean matchesKeyword(LyricEntry entry, String keyword) {
        if (!hasText(keyword)) {
            return true;
        }
        String normalized = keyword.trim().toLowerCase(Locale.ROOT);
        return containsIgnoreCase(entry.getTitle(), normalized)
                || containsIgnoreCase(entry.getBody(), normalized);
    }

    private boolean containsIgnoreCase(String value, String normalizedKeyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedKeyword);
    }

    private String normalizeBody(String body) {
        return body.trim().replace("\r\n", "\n");
    }

    private String preview(String body, int maxLength) {
        if (body == null || body.isBlank()) {
            return "";
        }
        return body.length() > maxLength ? body.substring(0, maxLength) + "..." : body;
    }

    private int countWords(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        return (int) List.of(value.trim().split("\\s+")).stream()
                .filter(word -> !word.isBlank())
                .count();
    }

    private LyricSummaryResponse toPublicSummary(LyricEntry entry) {
        return new LyricSummaryResponse(
                entry.getId(),
                null,
                entry.getProjectId(),
                entry.getTitle(),
                preview(entry.getBody(), 140),
                countWords(entry.getBody()),
                false,
                List.of(),
                entry.getCurrentVersion(),
                entry.getLanguage(),
                true,
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }

    private LyricResponse toPublicResponse(LyricEntry entry) {
        return new LyricResponse(
                entry.getId(),
                null,
                entry.getProjectId(),
                entry.getTitle(),
                entry.getBody(),
                entry.getCurrentVersion(),
                false,
                countWords(entry.getBody()),
                List.of(),
                entry.getVersions().stream()
                        .map(version -> new LyricVersionResponse(
                                version.getVersionNumber(),
                                version.getTitle(),
                                version.getBody(),
                                version.getEditedAt()
                        ))
                        .toList(),
                entry.getLanguage(),
                true,
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }

    private boolean isPublished(LyricEntry entry) {
        return entry.getReadyLibraryPublished() == null || Boolean.TRUE.equals(entry.getReadyLibraryPublished());
    }

    private boolean resolvePublishedOnCreate(Boolean published) {
        return published == null || Boolean.TRUE.equals(published);
    }

    private AdminReadyLibraryLyricSummaryResponse toAdminSummary(LyricEntry entry) {
        return new AdminReadyLibraryLyricSummaryResponse(
                entry.getId(),
                entry.getTitle(),
                preview(entry.getBody(), 120),
                entry.getLanguage(),
                entry.getCurrentVersion(),
                isPublished(entry),
                entry.getCreatedByAdminUserId(),
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }

    private AdminReadyLibraryLyricDetailResponse toAdminDetail(LyricEntry entry) {
        return new AdminReadyLibraryLyricDetailResponse(
                entry.getId(),
                entry.getProjectId(),
                entry.getTitle(),
                entry.getBody(),
                entry.getLanguage(),
                entry.getCurrentVersion(),
                isPublished(entry),
                entry.getCreatedByAdminUserId(),
                entry.getSourceLyricId(),
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

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
