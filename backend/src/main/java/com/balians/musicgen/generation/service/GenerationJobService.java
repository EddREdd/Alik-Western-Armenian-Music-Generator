package com.balians.musicgen.generation.service;

import com.balians.musicgen.auth.model.UserAccount;
import com.balians.musicgen.common.enums.GenerationModel;
import com.balians.musicgen.common.enums.InternalJobStatus;
import com.balians.musicgen.common.enums.ProviderJobStatus;
import com.balians.musicgen.common.exception.BadRequestException;
import com.balians.musicgen.common.exception.NotFoundException;
import com.balians.musicgen.generation.dto.CreateGenerationJobRequest;
import com.balians.musicgen.generation.dto.GenerationJobResponse;
import com.balians.musicgen.generation.dto.GenerationJobSummaryResponse;
import com.balians.musicgen.generation.mapper.GenerationJobMapper;
import com.balians.musicgen.generation.model.GenerationJob;
import com.balians.musicgen.generation.model.GenerationTrack;
import com.balians.musicgen.generation.model.JobStatusHistoryEntry;
import com.balians.musicgen.generation.repository.GenerationJobRepository;
import com.balians.musicgen.generation.repository.GenerationTrackRepository;
import com.balians.musicgen.lyrics.dto.LyricResponse;
import com.balians.musicgen.lyrics.service.LyricsService;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationJobService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final GenerationJobRepository generationJobRepository;
    private final GenerationTrackRepository generationTrackRepository;
    private final GenerationJobMapper generationJobMapper;
    private final LyricsService lyricsService;

    public GenerationJobResponse createJob(CreateGenerationJobRequest request) {
        return createJob(request, UserAccount.builder().id(null).build());
    }

    public GenerationJobResponse createJob(CreateGenerationJobRequest request, UserAccount owner) {
        validateGenerationRequest(request);
        LyricResponse lyric = null;
        if (hasText(request.lyricId())) {
            lyric = lyricsService.assertAvailableForGeneration(request.lyricId().trim());
        }

        GenerationJob job = GenerationJob.builder()
                .ownerUserId(owner.getId())
                .projectId(request.projectId().trim())
                .templateId(trimToNull(request.templateId()))
                .lyricId(lyric == null ? null : lyric.id())
                .lyricTitle(lyric == null ? null : lyric.title())
                .sourceType(request.sourceType())
                .internalStatus(InternalJobStatus.VALIDATED)
                .providerStatus(ProviderJobStatus.NOT_SUBMITTED)
                .promptFinal(request.promptFinal().trim())
                .styleFinal(trimToNull(request.styleFinal()))
                .titleFinal(trimToNull(request.titleFinal()))
                .customMode(request.customMode())
                .instrumental(request.instrumental())
                .model(request.model())
                .hiddenFromLibrary(false)
                .statusHistory(List.of(JobStatusHistoryEntry.builder()
                        .internalStatus(InternalJobStatus.VALIDATED)
                        .providerStatus(ProviderJobStatus.NOT_SUBMITTED)
                        .message("Job created and validated for future provider submission")
                        .changedAt(Instant.now())
                        .build()))
                .build();

        GenerationJob savedJob = generationJobRepository.save(job);
        if (lyric != null) {
            lyricsService.linkToSong(lyric.id(), savedJob.getId());
        }
        log.info("Created generation job id={} projectId={} sourceType={} model={}",
                savedJob.getId(), savedJob.getProjectId(), savedJob.getSourceType(), savedJob.getModel());

        return mapJob(savedJob, List.of());
    }

    public GenerationJobResponse getJobById(String id) {
        GenerationJob job = generationJobRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Generation job not found: " + id));
        List<GenerationTrack> tracks = generationTrackRepository.findByGenerationJobIdOrderByTrackIndexAsc(id);
        return mapJob(job, tracks);
    }

    public GenerationJobResponse getJobById(String id, String ownerUserId) {
        GenerationJob job = generationJobRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Generation job not found: " + id));
        assertJobOwnership(job, ownerUserId);
        List<GenerationTrack> tracks = generationTrackRepository.findByGenerationJobIdOrderByTrackIndexAsc(id);
        return mapJob(job, tracks);
    }

    public Page<GenerationJobSummaryResponse> listJobs(
            String projectId,
            InternalJobStatus internalStatus,
            ProviderJobStatus providerStatus,
            Integer page,
            Integer size
    ) {
        Pageable pageable = buildPageable(page, size);
        Page<GenerationJob> jobs;

        if (hasText(projectId) && internalStatus != null) {
            jobs = generationJobRepository.findByProjectIdAndInternalStatusAndHiddenFromLibraryFalse(projectId.trim(), internalStatus, pageable);
        } else if (hasText(projectId)) {
            jobs = generationJobRepository.findByProjectIdAndHiddenFromLibraryFalse(projectId.trim(), pageable);
        } else if (internalStatus != null) {
            jobs = generationJobRepository.findByInternalStatusAndHiddenFromLibraryFalse(internalStatus, pageable);
        } else if (providerStatus != null) {
            jobs = generationJobRepository.findByProviderStatusAndHiddenFromLibraryFalse(providerStatus, pageable);
        } else {
            jobs = generationJobRepository.findByHiddenFromLibraryFalse(pageable);
        }

        return jobs.map(this::mapSummary);
    }

    public Page<GenerationJobSummaryResponse> listJobs(
            String ownerUserId,
            String projectId,
            InternalJobStatus internalStatus,
            ProviderJobStatus providerStatus,
            Integer page,
            Integer size
    ) {
        Pageable pageable = buildPageable(page, size);
        Page<GenerationJob> jobs;

        if (hasText(projectId) && internalStatus != null) {
            jobs = generationJobRepository.findByOwnerUserIdAndProjectIdAndInternalStatusAndHiddenFromLibraryFalse(
                    ownerUserId,
                    projectId.trim(),
                    internalStatus,
                    pageable
            );
        } else if (hasText(projectId) && providerStatus != null) {
            jobs = generationJobRepository.findByOwnerUserIdAndProjectIdAndProviderStatusAndHiddenFromLibraryFalse(
                    ownerUserId,
                    projectId.trim(),
                    providerStatus,
                    pageable
            );
        } else if (hasText(projectId)) {
            jobs = generationJobRepository.findByOwnerUserIdAndProjectIdAndHiddenFromLibraryFalse(
                    ownerUserId,
                    projectId.trim(),
                    pageable
            );
        } else if (internalStatus != null) {
            jobs = generationJobRepository.findByOwnerUserIdAndInternalStatusAndHiddenFromLibraryFalse(
                    ownerUserId,
                    internalStatus,
                    pageable
            );
        } else if (providerStatus != null) {
            jobs = generationJobRepository.findByOwnerUserIdAndProviderStatusAndHiddenFromLibraryFalse(
                    ownerUserId,
                    providerStatus,
                    pageable
            );
        } else {
            jobs = generationJobRepository.findByOwnerUserIdAndHiddenFromLibraryFalse(ownerUserId, pageable);
        }

        return jobs.map(this::mapSummary);
    }

    public void deleteJob(String id) {
        GenerationJob job = generationJobRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Generation job not found: " + id));
        job.setHiddenFromLibrary(true);
        job.setUserDeletedAt(Instant.now());
        generationJobRepository.save(job);
        if (hasText(job.getLyricId())) {
            lyricsService.unlinkFromSong(job.getLyricId(), id);
        }
        log.info("Soft deleted generation job id={}", id);
    }

    public void deleteJob(String id, String ownerUserId) {
        GenerationJob job = generationJobRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Generation job not found: " + id));
        assertJobOwnership(job, ownerUserId);
        job.setHiddenFromLibrary(true);
        job.setUserDeletedAt(Instant.now());
        generationJobRepository.save(job);
        if (hasText(job.getLyricId())) {
            lyricsService.unlinkFromSong(job.getLyricId(), id);
        }
        log.info("Soft deleted generation job id={} ownerUserId={}", id, ownerUserId);
    }

    private void validateGenerationRequest(CreateGenerationJobRequest request) {
        if (request.model() != GenerationModel.V5) {
            throw new BadRequestException("Only V5 model is supported");
        }
        if (Boolean.TRUE.equals(request.customMode()) && !Boolean.TRUE.equals(request.instrumental()) && !hasText(request.promptFinal())) {
            throw new BadRequestException("promptFinal is required when customMode is true and instrumental is false");
        }
    }

    private Pageable buildPageable(Integer page, Integer size) {
        int safePage = page == null || page < 0 ? DEFAULT_PAGE : page;
        int safeSize = size == null || size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private GenerationJobSummaryResponse mapSummary(GenerationJob job) {
        return generationJobMapper.toSummary(job);
    }

    private GenerationJobResponse mapJob(GenerationJob job, List<GenerationTrack> tracks) {
        return generationJobMapper.toResponse(job, tracks);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private void assertJobOwnership(GenerationJob job, String ownerUserId) {
        if (ownerUserId == null || ownerUserId.isBlank()) {
            throw new NotFoundException("Generation job not found: " + job.getId());
        }
        if (job.getOwnerUserId() == null || !job.getOwnerUserId().equals(ownerUserId)) {
            throw new NotFoundException("Generation job not found: " + job.getId());
        }
    }
}
