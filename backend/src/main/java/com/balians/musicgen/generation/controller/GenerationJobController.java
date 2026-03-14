package com.balians.musicgen.generation.controller;

import com.balians.musicgen.auth.service.AuthService;
import com.balians.musicgen.common.enums.InternalJobStatus;
import com.balians.musicgen.common.enums.ProviderJobStatus;
import com.balians.musicgen.common.response.StandardSuccessResponse;
import com.balians.musicgen.generation.dto.CreateGenerationJobRequest;
import com.balians.musicgen.generation.dto.GenerationJobResponse;
import com.balians.musicgen.generation.dto.GenerationJobSummaryResponse;
import com.balians.musicgen.generation.service.GenerationJobService;
import com.balians.musicgen.generation.service.GenerationSubmissionService;
import com.balians.musicgen.polling.dto.PollAttemptResponse;
import com.balians.musicgen.polling.service.PollingReconciliationService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/generation-jobs")
public class GenerationJobController {

    private static final String SESSION_HEADER = "X-Session-Token";

    private final GenerationJobService generationJobService;
    private final GenerationSubmissionService generationSubmissionService;
    private final PollingReconciliationService pollingReconciliationService;
    private final AuthService authService;

    @PostMapping
    public StandardSuccessResponse<GenerationJobResponse> createJob(
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionToken,
            @Valid @RequestBody CreateGenerationJobRequest request
    ) {
        return StandardSuccessResponse.ok(
                generationJobService.createJob(request, authService.requireAuthenticatedUser(sessionToken))
        );
    }

    @GetMapping("/{id}")
    public StandardSuccessResponse<GenerationJobResponse> getJobById(@PathVariable String id) {
        return StandardSuccessResponse.ok(generationJobService.getJobById(id));
    }

    @PostMapping("/{id}/submit")
    public StandardSuccessResponse<GenerationJobResponse> submitJob(@PathVariable String id) {
        return StandardSuccessResponse.ok(generationSubmissionService.submitJob(id));
    }

    @DeleteMapping("/{id}")
    public StandardSuccessResponse<String> deleteJob(@PathVariable String id) {
        generationJobService.deleteJob(id);
        return StandardSuccessResponse.ok("deleted");
    }

    @PostMapping("/{id}/reconcile-now")
    public StandardSuccessResponse<GenerationJobResponse> reconcileNow(@PathVariable String id) {
        return StandardSuccessResponse.ok(pollingReconciliationService.reconcileNow(id));
    }

    @GetMapping("/{id}/poll-attempts")
    public StandardSuccessResponse<List<PollAttemptResponse>> getPollAttempts(@PathVariable String id) {
        return StandardSuccessResponse.ok(pollingReconciliationService.getPollAttempts(id));
    }

    @GetMapping
    public StandardSuccessResponse<Page<GenerationJobSummaryResponse>> listJobs(
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) InternalJobStatus internalStatus,
            @RequestParam(required = false) ProviderJobStatus providerStatus,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return StandardSuccessResponse.ok(
                generationJobService.listJobs(projectId, internalStatus, providerStatus, page, size)
        );
    }
}
