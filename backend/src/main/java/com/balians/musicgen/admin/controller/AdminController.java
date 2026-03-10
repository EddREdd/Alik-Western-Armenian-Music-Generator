package com.balians.musicgen.admin.controller;

import com.balians.musicgen.admin.dto.AdminGenerationJobFilter;
import com.balians.musicgen.admin.dto.AdminGenerationJobSummaryResponse;
import com.balians.musicgen.admin.dto.AdminHealthSummaryResponse;
import com.balians.musicgen.admin.dto.CallbackEventSummaryResponse;
import com.balians.musicgen.admin.dto.ManualActionResponse;
import com.balians.musicgen.common.exception.BadRequestException;
import com.balians.musicgen.common.enums.InternalJobStatus;
import com.balians.musicgen.common.enums.JobSourceType;
import com.balians.musicgen.common.enums.ProviderJobStatus;
import com.balians.musicgen.common.response.StandardSuccessResponse;
import com.balians.musicgen.config.FeatureFlagsProperties;
import com.balians.musicgen.generation.dto.GenerationJobResponse;
import com.balians.musicgen.generation.dto.GenerationTrackResponse;
import com.balians.musicgen.polling.dto.PollAttemptResponse;
import com.balians.musicgen.schedule.dto.ScheduleDefinitionResponse;
import com.balians.musicgen.schedule.dto.ScheduleRunResponse;
import com.balians.musicgen.schedule.service.ScheduleService;
import com.balians.musicgen.admin.service.AdminOperationsService;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminOperationsService adminOperationsService;
    private final ScheduleService scheduleService;
    private final FeatureFlagsProperties featureFlagsProperties;

    @GetMapping("/generation-jobs")
    public StandardSuccessResponse<Page<AdminGenerationJobSummaryResponse>> listJobs(
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) InternalJobStatus internalStatus,
            @RequestParam(required = false) ProviderJobStatus providerStatus,
            @RequestParam(required = false) JobSourceType sourceType,
            @RequestParam(required = false) String providerTaskId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @RequestParam(required = false) Boolean failedOnly,
            @RequestParam(required = false) Boolean stuckOnly,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        ensureAdminEnabled();
        return StandardSuccessResponse.ok(adminOperationsService.listGenerationJobs(
                new AdminGenerationJobFilter(projectId, internalStatus, providerStatus, sourceType, providerTaskId,
                        createdFrom, createdTo, failedOnly, stuckOnly, page, size)
        ));
    }

    @GetMapping("/generation-jobs/stuck")
    public StandardSuccessResponse<List<AdminGenerationJobSummaryResponse>> stuckJobs() {
        ensureAdminEnabled();
        return StandardSuccessResponse.ok(adminOperationsService.getStuckJobs());
    }

    @GetMapping("/generation-jobs/{id}")
    public StandardSuccessResponse<GenerationJobResponse> getJob(@PathVariable String id) {
        ensureAdminEnabled();
        return StandardSuccessResponse.ok(adminOperationsService.getGenerationJob(id));
    }

    @GetMapping("/generation-jobs/{id}/tracks")
    public StandardSuccessResponse<List<GenerationTrackResponse>> getTracks(@PathVariable String id) {
        ensureAdminEnabled();
        return StandardSuccessResponse.ok(adminOperationsService.getTracks(id));
    }

    @GetMapping("/generation-jobs/{id}/callback-events")
    public StandardSuccessResponse<List<CallbackEventSummaryResponse>> getCallbackEvents(@PathVariable String id) {
        ensureAdminEnabled();
        return StandardSuccessResponse.ok(adminOperationsService.getCallbackEvents(id));
    }

    @GetMapping("/generation-jobs/{id}/poll-attempts")
    public StandardSuccessResponse<List<PollAttemptResponse>> getPollAttempts(@PathVariable String id) {
        ensureAdminEnabled();
        return StandardSuccessResponse.ok(adminOperationsService.getPollAttempts(id));
    }

    @PostMapping("/generation-jobs/{id}/reconcile-now")
    public StandardSuccessResponse<GenerationJobResponse> reconcileNow(@PathVariable String id) {
        ensureAdminEnabled();
        return StandardSuccessResponse.ok(adminOperationsService.reconcileNow(id));
    }

    @PostMapping("/generation-jobs/{id}/retry-submit")
    public StandardSuccessResponse<GenerationJobResponse> retrySubmit(@PathVariable String id) {
        ensureAdminEnabled();
        return StandardSuccessResponse.ok(adminOperationsService.retrySubmit(id));
    }

    @PostMapping("/generation-jobs/{id}/mark-expired")
    public StandardSuccessResponse<ManualActionResponse> markExpired(@PathVariable String id) {
        ensureAdminEnabled();
        return StandardSuccessResponse.ok(adminOperationsService.markExpired(id));
    }

    @GetMapping("/schedules")
    public StandardSuccessResponse<List<ScheduleDefinitionResponse>> schedules() {
        ensureAdminEnabled();
        return StandardSuccessResponse.ok(adminOperationsService.getSchedules());
    }

    @GetMapping("/schedules/{id}")
    public StandardSuccessResponse<ScheduleDefinitionResponse> schedule(@PathVariable String id) {
        ensureAdminEnabled();
        return StandardSuccessResponse.ok(adminOperationsService.getSchedule(id));
    }

    @GetMapping("/schedules/{id}/runs")
    public StandardSuccessResponse<List<ScheduleRunResponse>> scheduleRuns(@PathVariable String id) {
        ensureAdminEnabled();
        return StandardSuccessResponse.ok(adminOperationsService.getScheduleRuns(id));
    }

    @PostMapping("/schedules/{id}/run-now")
    public StandardSuccessResponse<ScheduleRunResponse> runNow(@PathVariable String id) {
        ensureAdminEnabled();
        return StandardSuccessResponse.ok(scheduleService.runNow(id));
    }

    @GetMapping("/health/summary")
    public StandardSuccessResponse<AdminHealthSummaryResponse> healthSummary() {
        ensureAdminEnabled();
        return StandardSuccessResponse.ok(adminOperationsService.getHealthSummary());
    }

    private void ensureAdminEnabled() {
        if (!featureFlagsProperties.isAdminEndpointsEnabled()) {
            throw new BadRequestException("Admin endpoints are disabled by configuration");
        }
    }
}
