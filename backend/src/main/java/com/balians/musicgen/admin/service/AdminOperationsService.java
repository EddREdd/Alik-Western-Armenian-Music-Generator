package com.balians.musicgen.admin.service;

import com.balians.musicgen.admin.dto.AdminGenerationJobFilter;
import com.balians.musicgen.admin.dto.AdminGenerationJobSummaryResponse;
import com.balians.musicgen.admin.dto.AdminHealthSummaryResponse;
import com.balians.musicgen.admin.dto.CallbackEventSummaryResponse;
import com.balians.musicgen.admin.dto.ManualActionResponse;
import com.balians.musicgen.callback.model.CallbackEvent;
import com.balians.musicgen.callback.repository.CallbackEventRepository;
import com.balians.musicgen.common.enums.InternalJobStatus;
import com.balians.musicgen.common.enums.ProviderJobStatus;
import com.balians.musicgen.common.exception.BadRequestException;
import com.balians.musicgen.common.exception.ConflictException;
import com.balians.musicgen.common.exception.NotFoundException;
import com.balians.musicgen.generation.dto.GenerationJobResponse;
import com.balians.musicgen.generation.dto.GenerationTrackResponse;
import com.balians.musicgen.generation.mapper.GenerationJobMapper;
import com.balians.musicgen.generation.model.GenerationJob;
import com.balians.musicgen.generation.model.GenerationTrack;
import com.balians.musicgen.generation.model.JobStatusHistoryEntry;
import com.balians.musicgen.generation.repository.GenerationJobRepository;
import com.balians.musicgen.generation.repository.GenerationTrackRepository;
import com.balians.musicgen.generation.service.GenerationSubmissionService;
import com.balians.musicgen.polling.dto.PollAttemptResponse;
import com.balians.musicgen.polling.model.PollAttempt;
import com.balians.musicgen.polling.repository.PollAttemptRepository;
import com.balians.musicgen.polling.service.PollingReconciliationService;
import com.balians.musicgen.schedule.dto.ScheduleDefinitionResponse;
import com.balians.musicgen.schedule.dto.ScheduleRunResponse;
import com.balians.musicgen.schedule.model.ScheduleDefinition;
import com.balians.musicgen.schedule.repository.ScheduleDefinitionRepository;
import com.balians.musicgen.schedule.repository.ScheduleRunRepository;
import com.balians.musicgen.schedule.service.ScheduleService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOperationsService {

    private final GenerationJobRepository generationJobRepository;
    private final GenerationTrackRepository generationTrackRepository;
    private final CallbackEventRepository callbackEventRepository;
    private final PollAttemptRepository pollAttemptRepository;
    private final ScheduleDefinitionRepository scheduleDefinitionRepository;
    private final ScheduleRunRepository scheduleRunRepository;
    private final GenerationJobMapper generationJobMapper;
    private final GenerationSubmissionService generationSubmissionService;
    private final PollingReconciliationService pollingReconciliationService;
    private final ScheduleService scheduleService;
    private final OpsProperties opsProperties;

    public Page<AdminGenerationJobSummaryResponse> listGenerationJobs(AdminGenerationJobFilter filter) {
        validateFilter(filter);
        List<AdminGenerationJobSummaryResponse> items = generationJobRepository.findAll()
                .stream()
                .filter(job -> matches(job, filter))
                .sorted(Comparator.comparing(GenerationJob::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(job -> toAdminSummary(job, isStuck(job)))
                .toList();

        int safePage = filter.page() == null || filter.page() < 0 ? 0 : filter.page();
        int safeSize = filter.size() == null || filter.size() <= 0 ? 20 : Math.min(filter.size(), 100);
        int start = Math.min(safePage * safeSize, items.size());
        int end = Math.min(start + safeSize, items.size());
        return new PageImpl<>(items.subList(start, end), org.springframework.data.domain.PageRequest.of(safePage, safeSize), items.size());
    }

    public GenerationJobResponse getGenerationJob(String id) {
        GenerationJob job = getJob(id);
        return generationJobMapper.toResponse(job, generationTrackRepository.findByGenerationJobIdOrderByTrackIndexAsc(id));
    }

    public List<GenerationTrackResponse> getTracks(String jobId) {
        getJob(jobId);
        return generationTrackRepository.findByGenerationJobIdOrderByTrackIndexAsc(jobId)
                .stream()
                .map(this::mapTrack)
                .toList();
    }

    public List<CallbackEventSummaryResponse> getCallbackEvents(String jobId) {
        getJob(jobId);
        return callbackEventRepository.findByGenerationJobIdOrderByReceivedAtDesc(jobId)
                .stream()
                .map(this::mapCallbackEvent)
                .toList();
    }

    public List<PollAttemptResponse> getPollAttempts(String jobId) {
        getJob(jobId);
        return pollingReconciliationService.getPollAttempts(jobId);
    }

    public List<ScheduleDefinitionResponse> getSchedules() {
        return scheduleService.listSchedules();
    }

    public ScheduleDefinitionResponse getSchedule(String id) {
        return scheduleService.getSchedule(id);
    }

    public List<ScheduleRunResponse> getScheduleRuns(String id) {
        return scheduleService.getScheduleRuns(id);
    }

    public AdminHealthSummaryResponse getHealthSummary() {
        List<GenerationJob> jobs = generationJobRepository.findAll();
        Instant startOfToday = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        long stuckJobs = jobs.stream().filter(this::isStuck).count();
        Map<String, Long> jobsByInternal = jobs.stream()
                .filter(job -> job.getInternalStatus() != null)
                .collect(Collectors.groupingBy(job -> job.getInternalStatus().name(), Collectors.counting()));
        Map<String, Long> jobsByProvider = jobs.stream()
                .filter(job -> job.getProviderStatus() != null)
                .collect(Collectors.groupingBy(job -> job.getProviderStatus().name(), Collectors.counting()));

        log.info("Generated admin health summary");
        return new AdminHealthSummaryResponse(
                jobsByInternal,
                jobsByProvider,
                jobs.stream().filter(job -> job.getInternalStatus() == InternalJobStatus.FAILED).count(),
                stuckJobs,
                scheduleDefinitionRepository.findByEnabledTrueOrderByCreatedAtDesc().size(),
                scheduleDefinitionRepository.findByEnabledTrueAndNextRunAtLessThanEqualOrderByNextRunAtAsc(Instant.now()).size(),
                scheduleRunRepository.countByStatusAndStartedAtAfter(com.balians.musicgen.common.enums.ScheduleRunStatus.FAILED, startOfToday),
                callbackEventRepository.countByReceivedAtAfter(startOfToday),
                pollAttemptRepository.countByRequestedAtAfter(startOfToday)
        );
    }

    public List<AdminGenerationJobSummaryResponse> getStuckJobs() {
        List<AdminGenerationJobSummaryResponse> stuck = generationJobRepository.findAll()
                .stream()
                .filter(this::isStuck)
                .sorted(Comparator.comparing(GenerationJob::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(job -> toAdminSummary(job, true))
                .toList();
        log.info("Detected {} stuck job(s)", stuck.size());
        return stuck;
    }

    public GenerationJobResponse reconcileNow(String jobId) {
        log.info("Manual reconcile triggered for job id={}", jobId);
        return pollingReconciliationService.reconcileNow(jobId);
    }

    public GenerationJobResponse retrySubmit(String jobId) {
        GenerationJob job = getJob(jobId);
        if (job.getProviderTaskId() != null && !job.getProviderTaskId().isBlank()) {
            throw new ConflictException("retry-submit is only allowed when providerTaskId is absent");
        }
        if (job.getInternalStatus() != InternalJobStatus.FAILED
                && job.getInternalStatus() != InternalJobStatus.RETRY_PENDING
                && job.getInternalStatus() != InternalJobStatus.VALIDATED
                && job.getInternalStatus() != InternalJobStatus.EXPIRED) {
            throw new ConflictException("retry-submit is not allowed for the current job state");
        }

        job.setInternalStatus(InternalJobStatus.RETRY_PENDING);
        job.setProviderStatus(ProviderJobStatus.NOT_SUBMITTED);
        job.setFailedAt(null);
        job.setErrorCode(null);
        job.setErrorMessage(null);
        job.getStatusHistory().add(JobStatusHistoryEntry.builder()
                .internalStatus(InternalJobStatus.RETRY_PENDING)
                .providerStatus(ProviderJobStatus.NOT_SUBMITTED)
                .message("Manual retry requested by operator")
                .changedAt(Instant.now())
                .build());
        generationJobRepository.save(job);

        log.info("Manual retry triggered for job id={}", jobId);
        return generationSubmissionService.submitJob(jobId);
    }

    public ManualActionResponse markExpired(String jobId) {
        GenerationJob job = getJob(jobId);
        if (job.getInternalStatus() == InternalJobStatus.COMPLETED) {
            throw new ConflictException("Completed jobs cannot be marked expired");
        }

        job.setInternalStatus(InternalJobStatus.EXPIRED);
        job.setNextPollAt(null);
        job.getStatusHistory().add(JobStatusHistoryEntry.builder()
                .internalStatus(InternalJobStatus.EXPIRED)
                .providerStatus(job.getProviderStatus())
                .message("Marked expired by operator")
                .changedAt(Instant.now())
                .build());
        generationJobRepository.save(job);
        log.info("Marked job expired id={}", jobId);
        return new ManualActionResponse("mark-expired", jobId, "OK", "Generation job marked as expired");
    }

    private GenerationJob getJob(String id) {
        return generationJobRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Generation job not found: " + id));
    }

    private boolean matches(GenerationJob job, AdminGenerationJobFilter filter) {
        if (filter.projectId() != null && !filter.projectId().isBlank() && !filter.projectId().equals(job.getProjectId())) {
            return false;
        }
        if (filter.internalStatus() != null && filter.internalStatus() != job.getInternalStatus()) {
            return false;
        }
        if (filter.providerStatus() != null && filter.providerStatus() != job.getProviderStatus()) {
            return false;
        }
        if (filter.sourceType() != null && filter.sourceType() != job.getSourceType()) {
            return false;
        }
        if (filter.providerTaskId() != null && !filter.providerTaskId().isBlank()) {
            if (job.getProviderTaskId() == null || !job.getProviderTaskId().equals(filter.providerTaskId().trim())) {
                return false;
            }
        }
        if (filter.createdFrom() != null && (job.getCreatedAt() == null || job.getCreatedAt().isBefore(filter.createdFrom()))) {
            return false;
        }
        if (filter.createdTo() != null && (job.getCreatedAt() == null || job.getCreatedAt().isAfter(filter.createdTo()))) {
            return false;
        }
        if (Boolean.TRUE.equals(filter.failedOnly()) && job.getInternalStatus() != InternalJobStatus.FAILED) {
            return false;
        }
        if (Boolean.TRUE.equals(filter.stuckOnly()) && !isStuck(job)) {
            return false;
        }
        return true;
    }

    private void validateFilter(AdminGenerationJobFilter filter) {
        if (filter.createdFrom() != null && filter.createdTo() != null && filter.createdFrom().isAfter(filter.createdTo())) {
            throw new BadRequestException("createdFrom must be before createdTo");
        }
        if (opsProperties.getStuckThresholdMinutes() < 5 || opsProperties.getStuckThresholdMinutes() > 10080) {
            throw new BadRequestException("ops.stuckThresholdMinutes must be between 5 and 10080");
        }
    }

    private boolean isStuck(GenerationJob job) {
        if (job.getProviderTaskId() == null || job.getProviderTaskId().isBlank()) {
            return false;
        }
        if (job.getInternalStatus() != InternalJobStatus.SUBMITTED && job.getInternalStatus() != InternalJobStatus.IN_PROGRESS) {
            return false;
        }
        Instant reference = job.getLastPolledAt() != null ? job.getLastPolledAt() : job.getSubmittedAt() != null ? job.getSubmittedAt() : job.getCreatedAt();
        if (reference == null) {
            return false;
        }
        return reference.isBefore(Instant.now().minusSeconds(opsProperties.getStuckThresholdMinutes() * 60L));
    }

    private AdminGenerationJobSummaryResponse toAdminSummary(GenerationJob job, boolean stuck) {
        return new AdminGenerationJobSummaryResponse(
                job.getId(),
                job.getProjectId(),
                job.getTemplateId(),
                job.getSourceType(),
                job.getInternalStatus(),
                job.getProviderStatus(),
                job.getProviderTaskId(),
                job.getErrorCode(),
                job.getErrorMessage(),
                job.getCreatedAt(),
                job.getSubmittedAt(),
                job.getCompletedAt(),
                job.getFailedAt(),
                job.getNextPollAt(),
                job.getPollAttemptCount(),
                stuck
        );
    }

    private GenerationTrackResponse mapTrack(GenerationTrack track) {
        return new GenerationTrackResponse(
                track.getId(),
                track.getProviderMusicId(),
                track.getTrackIndex(),
                track.getAudioUrl(),
                track.getStreamAudioUrl(),
                track.getImageUrl(),
                track.getLyricsOrPrompt(),
                track.getTitle(),
                track.getTags(),
                track.getDurationSeconds(),
                track.getProviderCreateTime(),
                track.getAssetExpiryAt(),
                track.getSelectedFlag(),
                track.getCreatedAt()
        );
    }

    private CallbackEventSummaryResponse mapCallbackEvent(CallbackEvent event) {
        String payload = event.getPayloadJson();
        String preview = payload == null ? null : payload.substring(0, Math.min(payload.length(), opsProperties.getRawPayloadPreviewLength()));
        return new CallbackEventSummaryResponse(
                event.getId(),
                event.getProviderTaskId(),
                event.getCallbackType(),
                event.getCallbackCode(),
                event.getMessage(),
                event.getProcessingStatus(),
                event.getReceivedAt(),
                event.getProcessedAt(),
                event.getProcessingError(),
                preview
        );
    }
}
