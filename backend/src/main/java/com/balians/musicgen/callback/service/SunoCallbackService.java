package com.balians.musicgen.callback.service;

import com.balians.musicgen.callback.dto.SunoCallbackRequestDto;
import com.balians.musicgen.callback.model.CallbackEvent;
import com.balians.musicgen.callback.repository.CallbackEventRepository;
import com.balians.musicgen.common.enums.CallbackProcessingStatus;
import com.balians.musicgen.common.enums.InternalJobStatus;
import com.balians.musicgen.common.enums.ProviderJobStatus;
import com.balians.musicgen.generation.model.GenerationJob;
import com.balians.musicgen.generation.model.JobStatusHistoryEntry;
import com.balians.musicgen.generation.repository.GenerationJobRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SunoCallbackService {

    private static final Set<CallbackProcessingStatus> DUPLICATE_CHECK_STATUSES = Set.of(
            CallbackProcessingStatus.PROCESSED,
            CallbackProcessingStatus.DUPLICATE,
            CallbackProcessingStatus.JOB_NOT_FOUND
    );

    private final CallbackEventRepository callbackEventRepository;
    private final GenerationJobRepository generationJobRepository;
    private final GenerationTrackUpsertService generationTrackUpsertService;
    private final ObjectMapper objectMapper;

    public void handleCallback(String rawPayload) {
        log.info("Received Suno callback");

        JsonNode jsonNode = parsePayload(rawPayload);
        if (jsonNode == null) {
            persistInvalidPayload(rawPayload, null, "Malformed callback payload");
            return;
        }

        String canonicalPayload = toCanonicalJson(jsonNode);
        String payloadHash = hashPayload(canonicalPayload);

        SunoCallbackRequestDto request = parseDto(jsonNode);
        if (request == null || request.data() == null) {
            persistInvalidPayload(canonicalPayload, payloadHash, "Missing callback data block");
            return;
        }

        String taskId = trimToNull(request.data().taskId());
        String callbackType = normalizeCallbackType(request.data().callbackType());

        CallbackEvent callbackEvent = callbackEventRepository.save(CallbackEvent.builder()
                .providerTaskId(taskId)
                .callbackType(callbackType)
                .callbackCode(request.code() == null ? null : String.valueOf(request.code()))
                .message(trimToNull(request.msg()))
                .payloadJson(canonicalPayload)
                .payloadHash(payloadHash)
                .receivedAt(Instant.now())
                .processingStatus(CallbackProcessingStatus.RECEIVED)
                .build());

        log.info("Persisted callback event id={} taskId={} callbackType={}", callbackEvent.getId(), taskId, callbackType);

        if (isBlank(taskId) || isBlank(callbackType)) {
            markInvalid(callbackEvent, "Missing task_id or callbackType");
            return;
        }

        if (isDuplicate(callbackEvent)) {
            markDuplicate(callbackEvent);
            return;
        }

        Optional<GenerationJob> optionalJob = generationJobRepository.findByProviderTaskId(taskId);
        if (optionalJob.isEmpty()) {
            markJobNotFound(callbackEvent);
            return;
        }

        GenerationJob job = optionalJob.get();
        callbackEvent.setGenerationJobId(job.getId());
        callbackEventRepository.save(callbackEvent);

        try {
            processCallback(job, callbackEvent, request);
        } catch (Exception ex) {
            callbackEvent.setProcessingStatus(CallbackProcessingStatus.FAILED);
            callbackEvent.setProcessedAt(Instant.now());
            callbackEvent.setProcessingError(ex.getMessage());
            callbackEventRepository.save(callbackEvent);
            log.error("Failed to process callback event id={} taskId={}", callbackEvent.getId(), taskId, ex);
        }
    }

    private void processCallback(GenerationJob job, CallbackEvent callbackEvent, SunoCallbackRequestDto request) {
        String callbackType = callbackEvent.getCallbackType();

        if (request.code() != null && request.code() != 200 && !"error".equals(callbackType)) {
            markJobFailed(job, ProviderJobStatus.CALLBACK_EXCEPTION, "Provider callback returned non-success code");
            markProcessed(callbackEvent, null);
            return;
        }

        switch (callbackType) {
            case "text" -> {
                transitionJob(job, InternalJobStatus.IN_PROGRESS, ProviderJobStatus.TEXT_SUCCESS, "Text callback received");
                markProcessed(callbackEvent, null);
            }
            case "first" -> {
                int upserted = generationTrackUpsertService.upsertTracks(job, defaultTracks(request));
                transitionJob(job, InternalJobStatus.IN_PROGRESS, ProviderJobStatus.FIRST_SUCCESS,
                        "First-result callback received");
                markProcessed(callbackEvent, upserted == 0 ? null : "Upserted " + upserted + " track(s)");
            }
            case "complete" -> {
                List<?> tracks = defaultTracks(request);
                if (tracks.isEmpty()) {
                    markInvalid(callbackEvent, "Complete callback received without track data");
                    return;
                }
                int upserted = generationTrackUpsertService.upsertTracks(job, request.data().tracks());
                job.setCompletedAt(Instant.now());
                transitionJob(job, InternalJobStatus.COMPLETED, ProviderJobStatus.SUCCESS, "Complete callback received");
                markProcessed(callbackEvent, "Upserted " + upserted + " track(s)");
            }
            case "error" -> {
                ProviderJobStatus providerStatus = mapErrorStatus(request.msg());
                markJobFailed(job, providerStatus, trimToNull(request.msg()) == null ? "Provider error callback received" : request.msg().trim());
                markProcessed(callbackEvent, null);
            }
            default -> markInvalid(callbackEvent, "Unsupported callbackType: " + callbackType);
        }
    }

    private boolean isDuplicate(CallbackEvent callbackEvent) {
        return callbackEventRepository
                .findFirstByProviderTaskIdAndCallbackTypeAndPayloadHashAndProcessingStatusInOrderByReceivedAtDesc(
                        callbackEvent.getProviderTaskId(),
                        callbackEvent.getCallbackType(),
                        callbackEvent.getPayloadHash(),
                        DUPLICATE_CHECK_STATUSES
                )
                .isPresent();
    }

    private void markProcessed(CallbackEvent callbackEvent, String processingNote) {
        callbackEvent.setProcessingStatus(CallbackProcessingStatus.PROCESSED);
        callbackEvent.setProcessedAt(Instant.now());
        callbackEvent.setProcessingError(processingNote);
        callbackEventRepository.save(callbackEvent);
        log.info("Processed callback event id={} taskId={} callbackType={}",
                callbackEvent.getId(), callbackEvent.getProviderTaskId(), callbackEvent.getCallbackType());
    }

    private void markDuplicate(CallbackEvent callbackEvent) {
        callbackEvent.setProcessingStatus(CallbackProcessingStatus.DUPLICATE);
        callbackEvent.setProcessedAt(Instant.now());
        callbackEventRepository.save(callbackEvent);
        log.info("Skipped duplicate callback event id={} taskId={} callbackType={}",
                callbackEvent.getId(), callbackEvent.getProviderTaskId(), callbackEvent.getCallbackType());
    }

    private void markJobNotFound(CallbackEvent callbackEvent) {
        callbackEvent.setProcessingStatus(CallbackProcessingStatus.JOB_NOT_FOUND);
        callbackEvent.setProcessedAt(Instant.now());
        callbackEvent.setProcessingError("No generation job found for providerTaskId");
        callbackEventRepository.save(callbackEvent);
        log.warn("No generation job found for callback event id={} taskId={}",
                callbackEvent.getId(), callbackEvent.getProviderTaskId());
    }

    private void markInvalid(CallbackEvent callbackEvent, String reason) {
        callbackEvent.setProcessingStatus(CallbackProcessingStatus.INVALID_PAYLOAD);
        callbackEvent.setProcessedAt(Instant.now());
        callbackEvent.setProcessingError(reason);
        callbackEventRepository.save(callbackEvent);
        log.warn("Invalid callback event id={} reason={}", callbackEvent.getId(), reason);
    }

    private void persistInvalidPayload(String payloadJson, String payloadHash, String reason) {
        callbackEventRepository.save(CallbackEvent.builder()
                .payloadJson(payloadJson)
                .payloadHash(payloadHash)
                .receivedAt(Instant.now())
                .processingStatus(CallbackProcessingStatus.INVALID_PAYLOAD)
                .processedAt(Instant.now())
                .processingError(reason)
                .build());
        log.warn("Stored invalid Suno callback payload reason={}", reason);
    }

    private void transitionJob(GenerationJob job, InternalJobStatus internalStatus, ProviderJobStatus providerStatus, String message) {
        job.setInternalStatus(internalStatus);
        job.setProviderStatus(providerStatus);
        if (internalStatus == InternalJobStatus.COMPLETED || internalStatus == InternalJobStatus.FAILED) {
            job.setNextPollAt(null);
        }
        appendHistory(job, internalStatus, providerStatus, message);
        generationJobRepository.save(job);
    }

    private void markJobFailed(GenerationJob job, ProviderJobStatus providerStatus, String message) {
        job.setInternalStatus(InternalJobStatus.FAILED);
        job.setProviderStatus(providerStatus);
        job.setErrorCode(providerStatus.name());
        job.setErrorMessage(message);
        job.setFailedAt(Instant.now());
        job.setNextPollAt(null);
        appendHistory(job, InternalJobStatus.FAILED, providerStatus, message);
        generationJobRepository.save(job);
    }

    private void appendHistory(GenerationJob job, InternalJobStatus internalStatus, ProviderJobStatus providerStatus, String message) {
        job.getStatusHistory().add(JobStatusHistoryEntry.builder()
                .internalStatus(internalStatus)
                .providerStatus(providerStatus)
                .message(message)
                .changedAt(Instant.now())
                .build());
    }

    private ProviderJobStatus mapErrorStatus(String message) {
        if (message != null && message.toLowerCase().contains("sensitive")) {
            return ProviderJobStatus.SENSITIVE_WORD_ERROR;
        }
        return ProviderJobStatus.CALLBACK_EXCEPTION;
    }

    private JsonNode parsePayload(String rawPayload) {
        try {
            return objectMapper.readTree(rawPayload);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private SunoCallbackRequestDto parseDto(JsonNode jsonNode) {
        try {
            return objectMapper.treeToValue(jsonNode, SunoCallbackRequestDto.class);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private String toCanonicalJson(JsonNode jsonNode) {
        try {
            return objectMapper.writeValueAsString(jsonNode);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize callback payload", ex);
        }
    }

    private String hashPayload(String payloadJson) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payloadJson.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private List<com.balians.musicgen.callback.dto.SunoCallbackTrackDto> defaultTracks(SunoCallbackRequestDto request) {
        if (request.data() == null || request.data().tracks() == null) {
            return List.of();
        }
        return request.data().tracks();
    }

    private String normalizeCallbackType(String callbackType) {
        String value = trimToNull(callbackType);
        return value == null ? null : value.toLowerCase();
    }

    private String trimToNull(String value) {
        if (isBlank(value)) {
            return null;
        }
        return value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
