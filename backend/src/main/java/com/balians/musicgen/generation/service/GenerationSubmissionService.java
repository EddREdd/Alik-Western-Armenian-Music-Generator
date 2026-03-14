package com.balians.musicgen.generation.service;

import com.balians.musicgen.auth.model.UserAccount;
import com.balians.musicgen.auth.repository.UserAccountRepository;
import com.balians.musicgen.auth.service.AccountCreditsService;
import com.balians.musicgen.common.enums.InternalJobStatus;
import com.balians.musicgen.common.enums.ProviderJobStatus;
import com.balians.musicgen.common.exception.BadRequestException;
import com.balians.musicgen.common.exception.NotFoundException;
import com.balians.musicgen.common.exception.ProviderIntegrationException;
import com.balians.musicgen.config.FeatureFlagsProperties;
import com.balians.musicgen.generation.dto.GenerationJobResponse;
import com.balians.musicgen.generation.mapper.GenerationJobMapper;
import com.balians.musicgen.generation.model.GenerationJob;
import com.balians.musicgen.generation.model.JobStatusHistoryEntry;
import com.balians.musicgen.generation.repository.GenerationJobRepository;
import com.balians.musicgen.generation.repository.GenerationTrackRepository;
import com.balians.musicgen.provider.client.SunoClient;
import com.balians.musicgen.provider.config.ProviderProperties;
import com.balians.musicgen.provider.dto.SunoGenerateRequest;
import com.balians.musicgen.provider.dto.SunoGenerateResponse;
import com.balians.musicgen.provider.service.ProviderSubmissionValidator;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationSubmissionService {

    private final GenerationJobRepository generationJobRepository;
    private final GenerationTrackRepository generationTrackRepository;
    private final GenerationJobMapper generationJobMapper;
    private final ProviderSubmissionValidator providerSubmissionValidator;
    private final ProviderProperties providerProperties;
    private final FeatureFlagsProperties featureFlagsProperties;
    private final SunoClient sunoClient;
    private final WesternArmenianLyricsTransformer westernArmenianLyricsTransformer;
    private final UserAccountRepository userAccountRepository;
    private final AccountCreditsService accountCreditsService;

    public GenerationJobResponse submitJob(String id) {
        if (!featureFlagsProperties.isProviderSubmissionEnabled()) {
            throw new BadRequestException("Provider submission is disabled by configuration");
        }
        GenerationJob job = generationJobRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Generation job not found: " + id));

        validateSubmissionAllowed(job);
        providerSubmissionValidator.validateForSubmission(job);

        SunoGenerateRequest request = new SunoGenerateRequest(
                transformLyricsForProvider(job.getPromptFinal()),
                job.getCustomMode(),
                job.getInstrumental(),
                providerSubmissionValidator.toProviderModel(job.getModel()),
                normalize(job.getStyleFinal()),
                normalize(job.getTitleFinal()),
                buildCallbackUrl()
        );

        log.info("Submitting generation job id={} customMode={} instrumental={} model={}",
                job.getId(), job.getCustomMode(), job.getInstrumental(), job.getModel());

        try {
            SunoGenerateResponse response = sunoClient.submitGeneration(request);
            String taskId = extractTaskId(response);
            consumeCreditIfOwned(job);

            job.setProviderTaskId(taskId);
            job.setInternalStatus(InternalJobStatus.SUBMITTED);
            job.setProviderStatus(ProviderJobStatus.PENDING);
            job.setSubmittedAt(Instant.now());
            job.setNextPollAt(Instant.now());
            job.setPollAttemptCount(0);
            job.setErrorCode(null);
            job.setErrorMessage(null);
            appendHistory(job, InternalJobStatus.SUBMITTED, ProviderJobStatus.PENDING, "Job submitted to Suno provider");

            GenerationJob savedJob = generationJobRepository.save(job);
            log.info("Submitted generation job id={} taskId={}", savedJob.getId(), taskId);

            return generationJobMapper.toResponse(
                    savedJob,
                    generationTrackRepository.findByGenerationJobIdOrderByTrackIndexAsc(savedJob.getId())
            );
        } catch (ProviderIntegrationException ex) {
            markSubmissionFailure(job, ex.getErrorCode(), ex.getMessage());
            throw ex;
        } catch (RuntimeException ex) {
            markSubmissionFailure(job, "PROVIDER_SUBMISSION_ERROR", "Provider submission failed unexpectedly");
            throw new ProviderIntegrationException(HttpStatus.BAD_GATEWAY, "PROVIDER_SUBMISSION_ERROR",
                    "Provider submission failed unexpectedly");
        }
    }

    private void validateSubmissionAllowed(GenerationJob job) {
        if (job.getInternalStatus() != InternalJobStatus.VALIDATED && job.getInternalStatus() != InternalJobStatus.RETRY_PENDING) {
            throw new BadRequestException("Only VALIDATED or RETRY_PENDING jobs can be submitted");
        }
        if (job.getProviderTaskId() != null && !job.getProviderTaskId().isBlank()) {
            throw new BadRequestException("Generation job is already linked to a provider task");
        }
    }

    private String extractTaskId(SunoGenerateResponse response) {
        if (response.code() == null || response.code() != 200) {
            throw new ProviderIntegrationException(HttpStatus.BAD_GATEWAY, "PROVIDER_REJECTED_REQUEST",
                    response.msg() == null ? "Provider rejected the request" : response.msg());
        }
        if (response.data() == null || response.data().taskId() == null || response.data().taskId().isBlank()) {
            throw new ProviderIntegrationException(HttpStatus.BAD_GATEWAY, "PROVIDER_MISSING_TASK_ID",
                    "Provider response did not include taskId");
        }
        return response.data().taskId().trim();
    }

    private void markSubmissionFailure(GenerationJob job, String errorCode, String errorMessage) {
        job.setInternalStatus(InternalJobStatus.FAILED);
        job.setProviderStatus(ProviderJobStatus.CREATE_TASK_FAILED);
        job.setErrorCode(errorCode);
        job.setErrorMessage(errorMessage);
        job.setFailedAt(Instant.now());
        appendHistory(job, InternalJobStatus.FAILED, ProviderJobStatus.CREATE_TASK_FAILED,
                "Provider submission failed: " + errorCode);
        generationJobRepository.save(job);
        log.warn("Generation job submission failed id={} errorCode={}", job.getId(), errorCode);
    }

    private void appendHistory(GenerationJob job, InternalJobStatus internalStatus, ProviderJobStatus providerStatus, String message) {
        job.getStatusHistory().add(JobStatusHistoryEntry.builder()
                .internalStatus(internalStatus)
                .providerStatus(providerStatus)
                .message(message)
                .changedAt(Instant.now())
                .build());
    }

    private String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private String buildCallbackUrl() {
        String base = providerProperties.getCallbackBaseUrl();
        return base.endsWith("/") ? base + "api/v1/integrations/suno/callback" : base + "/api/v1/integrations/suno/callback";
    }

    private String transformLyricsForProvider(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        return westernArmenianLyricsTransformer.transform(normalized);
    }

    private void consumeCreditIfOwned(GenerationJob job) {
        if (job.getOwnerUserId() == null || job.getOwnerUserId().isBlank()) {
            return;
        }

        UserAccount owner = userAccountRepository.findById(job.getOwnerUserId())
                .orElseThrow(() -> new NotFoundException("Owner user not found for generation job: " + job.getOwnerUserId()));
        accountCreditsService.consumeGenerationCredit(owner);
    }
}
