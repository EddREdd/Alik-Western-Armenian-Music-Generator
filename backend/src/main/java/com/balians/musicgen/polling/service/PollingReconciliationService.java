package com.balians.musicgen.polling.service;

import com.balians.musicgen.callback.service.GenerationTrackUpsertService;
import com.balians.musicgen.common.enums.InternalJobStatus;
import com.balians.musicgen.common.enums.ProviderJobStatus;
import com.balians.musicgen.common.exception.BadRequestException;
import com.balians.musicgen.common.exception.NotFoundException;
import com.balians.musicgen.generation.dto.GenerationJobResponse;
import com.balians.musicgen.generation.mapper.GenerationJobMapper;
import com.balians.musicgen.generation.model.GenerationJob;
import com.balians.musicgen.generation.model.JobStatusHistoryEntry;
import com.balians.musicgen.generation.repository.GenerationJobRepository;
import com.balians.musicgen.generation.repository.GenerationTrackRepository;
import com.balians.musicgen.polling.dto.PollAttemptResponse;
import com.balians.musicgen.polling.model.PollAttempt;
import com.balians.musicgen.polling.model.PollAttemptOutcome;
import com.balians.musicgen.polling.repository.PollAttemptRepository;
import com.balians.musicgen.provider.client.SunoClient;
import com.balians.musicgen.provider.dto.SunoRecordInfoDataDto;
import com.balians.musicgen.provider.dto.SunoRecordInfoResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PollingReconciliationService {

    private static final List<InternalJobStatus> ACTIVE_STATUSES = List.of(
            InternalJobStatus.SUBMITTED,
            InternalJobStatus.IN_PROGRESS
    );

    private final GenerationJobRepository generationJobRepository;
    private final GenerationTrackRepository generationTrackRepository;
    private final PollAttemptRepository pollAttemptRepository;
    private final GenerationJobMapper generationJobMapper;
    private final GenerationTrackUpsertService generationTrackUpsertService;
    private final PollingProperties pollingProperties;
    private final SunoClient sunoClient;
    private final ObjectMapper objectMapper;

    public void pollDueJobs() {
        Instant now = Instant.now();
        List<GenerationJob> candidates = generationJobRepository
                .findByInternalStatusInAndProviderTaskIdIsNotNullOrderByNextPollAtAscCreatedAtAsc(
                        ACTIVE_STATUSES,
                        PageRequest.of(0, pollingProperties.getBatchSize())
                )
                .stream()
                .filter(job -> job.getNextPollAt() == null || !job.getNextPollAt().isAfter(now))
                .toList();

        log.info("Polling cycle started; found {} candidate job(s)", candidates.size());
        for (GenerationJob job : candidates) {
            pollJob(job);
        }
        log.info("Polling cycle finished");
    }

    public GenerationJobResponse reconcileNow(String jobId) {
        GenerationJob job = generationJobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Generation job not found: " + jobId));
        if (job.getProviderTaskId() == null || job.getProviderTaskId().isBlank()) {
            throw new BadRequestException("Generation job does not have a providerTaskId");
        }
        pollJob(job);
        GenerationJob refreshedJob = generationJobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Generation job not found after reconciliation: " + jobId));
        return generationJobMapper.toResponse(
                refreshedJob,
                generationTrackRepository.findByGenerationJobIdOrderByTrackIndexAsc(jobId)
        );
    }

    public List<PollAttemptResponse> getPollAttempts(String jobId) {
        return pollAttemptRepository.findByGenerationJobIdOrderByRequestedAtDesc(jobId)
                .stream()
                .map(attempt -> new PollAttemptResponse(
                        attempt.getId(),
                        attempt.getGenerationJobId(),
                        attempt.getProviderTaskId(),
                        attempt.getAttemptNo(),
                        attempt.getRequestedAt(),
                        attempt.getProviderStatus(),
                        attempt.getResponseCode(),
                        attempt.getNextPollAt(),
                        attempt.getOutcome(),
                        attempt.getErrorMessage()
                ))
                .toList();
    }

    private void pollJob(GenerationJob job) {
        Instant requestedAt = Instant.now();
        int attemptNo = (job.getPollAttemptCount() == null ? 0 : job.getPollAttemptCount()) + 1;

        log.info("Polling generation job id={} taskId={} attemptNo={}", job.getId(), job.getProviderTaskId(), attemptNo);

        try {
            SunoRecordInfoResponse response = sunoClient.getRecordInfo(job.getProviderTaskId());
            handleProviderResponse(job, attemptNo, requestedAt, response);
        } catch (Exception ex) {
            Instant nextPollAt = calculateNextPollAt(attemptNo, requestedAt);
            job.setInternalStatus(InternalJobStatus.IN_PROGRESS);
            job.setLastPolledAt(requestedAt);
            job.setPollAttemptCount(attemptNo);
            job.setNextPollAt(nextPollAt);
            generationJobRepository.save(job);

            persistAttempt(job, attemptNo, requestedAt, null, null, nextPollAt, PollAttemptOutcome.PROVIDER_ERROR, ex.getMessage());
            log.warn("Polling failed for job id={} taskId={} attemptNo={}", job.getId(), job.getProviderTaskId(), attemptNo, ex);
        }
    }

    private void handleProviderResponse(GenerationJob job, int attemptNo, Instant requestedAt, SunoRecordInfoResponse response) {
        String responseJson = serializeResponse(response);

        if (response == null || response.data() == null || isBlank(response.data().status())) {
            Instant nextPollAt = calculateNextPollAt(attemptNo, requestedAt);
            job.setLastPolledAt(requestedAt);
            job.setPollAttemptCount(attemptNo);
            job.setNextPollAt(nextPollAt);
            generationJobRepository.save(job);
            persistAttempt(job, attemptNo, requestedAt, null, response == null ? null : response.code(), nextPollAt,
                    PollAttemptOutcome.MALFORMED_RESPONSE, "Missing provider data or status", responseJson);
            return;
        }

        SunoRecordInfoDataDto data = response.data();
        if (isBlank(data.taskId()) || !job.getProviderTaskId().equals(data.taskId().trim())) {
            Instant nextPollAt = calculateNextPollAt(attemptNo, requestedAt);
            job.setInternalStatus(InternalJobStatus.IN_PROGRESS);
            job.setLastPolledAt(requestedAt);
            job.setPollAttemptCount(attemptNo);
            job.setNextPollAt(nextPollAt);
            generationJobRepository.save(job);
            persistAttempt(job, attemptNo, requestedAt, data.status(), response.code(), nextPollAt,
                    PollAttemptOutcome.MALFORMED_RESPONSE, "Missing or mismatched taskId in provider response", responseJson);
            return;
        }
        ProviderJobStatus providerStatus = safeProviderStatus(data.status());
        if (providerStatus == null) {
            Instant nextPollAt = calculateNextPollAt(attemptNo, requestedAt);
            job.setInternalStatus(InternalJobStatus.IN_PROGRESS);
            job.setLastPolledAt(requestedAt);
            job.setPollAttemptCount(attemptNo);
            job.setNextPollAt(nextPollAt);
            appendHistory(job, InternalJobStatus.IN_PROGRESS, job.getProviderStatus(), "Unknown provider status from poll: " + data.status());
            generationJobRepository.save(job);
            persistAttempt(job, attemptNo, requestedAt, data.status(), response.code(), nextPollAt,
                    PollAttemptOutcome.MALFORMED_RESPONSE, "Unknown provider status", responseJson);
            return;
        }

        if (providerStatus == ProviderJobStatus.SUCCESS) {
            int upserted = generationTrackUpsertService.upsertProviderTracks(
                    job,
                    data.response() == null ? List.of() : data.response().sunoData()
            );

            if (upserted == 0) {
                Instant nextPollAt = calculateNextPollAt(attemptNo, requestedAt);
                job.setInternalStatus(InternalJobStatus.IN_PROGRESS);
                job.setProviderStatus(providerStatus);
                job.setLastPolledAt(requestedAt);
                job.setPollAttemptCount(attemptNo);
                job.setNextPollAt(nextPollAt);
                appendHistory(job, InternalJobStatus.IN_PROGRESS, providerStatus, "Provider returned SUCCESS without track data");
                generationJobRepository.save(job);
                persistAttempt(job, attemptNo, requestedAt, providerStatus.name(), response.code(), nextPollAt,
                        PollAttemptOutcome.MALFORMED_RESPONSE, "SUCCESS without track data", responseJson);
                return;
            }

            job.setInternalStatus(InternalJobStatus.COMPLETED);
            job.setProviderStatus(providerStatus);
            job.setCompletedAt(requestedAt);
            job.setErrorCode(null);
            job.setErrorMessage(null);
            job.setLastPolledAt(requestedAt);
            job.setPollAttemptCount(attemptNo);
            job.setNextPollAt(null);
            appendHistory(job, InternalJobStatus.COMPLETED, providerStatus, "Job completed via polling reconciliation");
            generationJobRepository.save(job);
            persistAttempt(job, attemptNo, requestedAt, providerStatus.name(), response.code(), null,
                    PollAttemptOutcome.TERMINAL_SUCCESS, null, responseJson);
            return;
        }

        if (isTerminalFailure(providerStatus)) {
            job.setInternalStatus(InternalJobStatus.FAILED);
            job.setProviderStatus(providerStatus);
            job.setErrorCode(data.errorCode() == null ? providerStatus.name() : String.valueOf(data.errorCode()));
            job.setErrorMessage(isBlank(data.errorMessage()) ? response.msg() : data.errorMessage());
            job.setFailedAt(requestedAt);
            job.setLastPolledAt(requestedAt);
            job.setPollAttemptCount(attemptNo);
            job.setNextPollAt(null);
            appendHistory(job, InternalJobStatus.FAILED, providerStatus, "Job failed via polling reconciliation");
            generationJobRepository.save(job);
            persistAttempt(job, attemptNo, requestedAt, providerStatus.name(), response.code(), null,
                    PollAttemptOutcome.TERMINAL_FAILURE, job.getErrorMessage(), responseJson);
            return;
        }

        int upserted = generationTrackUpsertService.upsertProviderTracks(
                job,
                data.response() == null ? List.of() : data.response().sunoData()
        );
        Instant nextPollAt = calculateNextPollAt(attemptNo, requestedAt);
        job.setInternalStatus(InternalJobStatus.IN_PROGRESS);
        job.setProviderStatus(providerStatus);
        job.setLastPolledAt(requestedAt);
        job.setPollAttemptCount(attemptNo);
        job.setNextPollAt(nextPollAt);
        appendHistory(job, InternalJobStatus.IN_PROGRESS, providerStatus, "Job reconciled by polling");
        generationJobRepository.save(job);
        persistAttempt(job, attemptNo, requestedAt, providerStatus.name(), response.code(), nextPollAt,
                PollAttemptOutcome.PARTIAL, upserted == 0 ? null : "Upserted " + upserted + " track(s)", responseJson);
    }

    private void persistAttempt(
            GenerationJob job,
            int attemptNo,
            Instant requestedAt,
            String providerStatus,
            Integer responseCode,
            Instant nextPollAt,
            PollAttemptOutcome outcome,
            String errorMessage
    ) {
        persistAttempt(job, attemptNo, requestedAt, providerStatus, responseCode, nextPollAt, outcome, errorMessage, null);
    }

    private void persistAttempt(
            GenerationJob job,
            int attemptNo,
            Instant requestedAt,
            String providerStatus,
            Integer responseCode,
            Instant nextPollAt,
            PollAttemptOutcome outcome,
            String errorMessage,
            String responseJson
    ) {
        pollAttemptRepository.save(PollAttempt.builder()
                .generationJobId(job.getId())
                .providerTaskId(job.getProviderTaskId())
                .attemptNo(attemptNo)
                .requestedAt(requestedAt)
                .providerStatus(providerStatus)
                .responseCode(responseCode)
                .responseJson(responseJson)
                .nextPollAt(nextPollAt)
                .outcome(outcome)
                .errorMessage(errorMessage)
                .build());
    }

    private Instant calculateNextPollAt(int attemptNo, Instant now) {
        long delaySeconds = pollingProperties.getBaseDelaySeconds() * (1L << Math.max(0, Math.min(attemptNo - 1, 8)));
        delaySeconds = Math.min(delaySeconds, pollingProperties.getMaxDelaySeconds());
        return now.plusSeconds(delaySeconds);
    }

    private ProviderJobStatus safeProviderStatus(String status) {
        try {
            return ProviderJobStatus.valueOf(status.trim().toUpperCase());
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean isTerminalFailure(ProviderJobStatus status) {
        return status == ProviderJobStatus.CREATE_TASK_FAILED
                || status == ProviderJobStatus.GENERATE_AUDIO_FAILED
                || status == ProviderJobStatus.SENSITIVE_WORD_ERROR;
    }

    private void appendHistory(GenerationJob job, InternalJobStatus internalStatus, ProviderJobStatus providerStatus, String message) {
        job.getStatusHistory().add(JobStatusHistoryEntry.builder()
                .internalStatus(internalStatus)
                .providerStatus(providerStatus)
                .message(message)
                .changedAt(Instant.now())
                .build());
    }

    private String serializeResponse(SunoRecordInfoResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
