package com.balians.musicgen.admin.controller;

import com.balians.musicgen.admin.dto.AdminDashboardResponse;
import com.balians.musicgen.admin.dto.AdminGenerationJobFilter;
import com.balians.musicgen.admin.dto.AdminGenerationJobSummaryResponse;
import com.balians.musicgen.admin.dto.AdminHealthSummaryResponse;
import com.balians.musicgen.admin.dto.AdminInviteCodeResponse;
import com.balians.musicgen.admin.dto.AdminLyricDetailResponse;
import com.balians.musicgen.admin.dto.AdminLyricSummaryResponse;
import com.balians.musicgen.admin.dto.AdminSongDetailResponse;
import com.balians.musicgen.admin.dto.AdminSongSummaryResponse;
import com.balians.musicgen.admin.dto.AdminUserDetailResponse;
import com.balians.musicgen.admin.dto.AdminUserSummaryResponse;
import com.balians.musicgen.admin.dto.CallbackEventSummaryResponse;
import com.balians.musicgen.admin.dto.FreezeUserRequest;
import com.balians.musicgen.admin.dto.GenerateInviteCodesRequest;
import com.balians.musicgen.admin.dto.ManualActionResponse;
import com.balians.musicgen.admin.dto.SecurityLogResponse;
import com.balians.musicgen.admin.service.AdminOperationsService;
import com.balians.musicgen.auth.service.AuthService;
import com.balians.musicgen.common.enums.InternalJobStatus;
import com.balians.musicgen.common.enums.JobSourceType;
import com.balians.musicgen.common.enums.ProviderJobStatus;
import com.balians.musicgen.common.exception.BadRequestException;
import com.balians.musicgen.common.response.StandardSuccessResponse;
import com.balians.musicgen.config.FeatureFlagsProperties;
import com.balians.musicgen.generation.dto.GenerationJobResponse;
import com.balians.musicgen.generation.dto.GenerationTrackResponse;
import com.balians.musicgen.polling.dto.PollAttemptResponse;
import com.balians.musicgen.schedule.dto.ScheduleDefinitionResponse;
import com.balians.musicgen.schedule.dto.ScheduleRunResponse;
import com.balians.musicgen.schedule.service.ScheduleService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminController {

    private static final String SESSION_HEADER = "X-Session-Token";

    private final AdminOperationsService adminOperationsService;
    private final ScheduleService scheduleService;
    private final FeatureFlagsProperties featureFlagsProperties;
    private final AuthService authService;

    @GetMapping("/dashboard")
    public StandardSuccessResponse<AdminDashboardResponse> dashboard(
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionToken
    ) {
        ensureAdminAccess(sessionToken);
        return StandardSuccessResponse.ok(adminOperationsService.getDashboard());
    }

    @GetMapping("/generation-jobs")
    public StandardSuccessResponse<Page<AdminGenerationJobSummaryResponse>> listJobs(
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionToken,
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
        ensureAdminAccess(sessionToken);
        return StandardSuccessResponse.ok(adminOperationsService.listGenerationJobs(
                new AdminGenerationJobFilter(projectId, internalStatus, providerStatus, sourceType, providerTaskId,
                        createdFrom, createdTo, failedOnly, stuckOnly, page, size)
        ));
    }

    @GetMapping("/generation-jobs/stuck")
    public StandardSuccessResponse<List<AdminGenerationJobSummaryResponse>> stuckJobs(
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionToken
    ) {
        ensureAdminAccess(sessionToken);
        return StandardSuccessResponse.ok(adminOperationsService.getStuckJobs());
    }

    @GetMapping("/generation-jobs/{id}")
    public StandardSuccessResponse<GenerationJobResponse> getJob(
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionToken,
            @PathVariable String id
    ) {
        ensureAdminAccess(sessionToken);
        return StandardSuccessResponse.ok(adminOperationsService.getGenerationJob(id));
    }

    @GetMapping("/generation-jobs/{id}/tracks")
    public StandardSuccessResponse<List<GenerationTrackResponse>> getTracks(
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionToken,
            @PathVariable String id
    ) {
        ensureAdminAccess(sessionToken);
        return StandardSuccessResponse.ok(adminOperationsService.getTracks(id));
    }

    @GetMapping("/generation-jobs/{id}/callback-events")
    public StandardSuccessResponse<List<CallbackEventSummaryResponse>> getCallbackEvents(
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionToken,
            @PathVariable String id
    ) {
        ensureAdminAccess(sessionToken);
        return StandardSuccessResponse.ok(adminOperationsService.getCallbackEvents(id));
    }

    @GetMapping("/generation-jobs/{id}/poll-attempts")
    public StandardSuccessResponse<List<PollAttemptResponse>> getPollAttempts(
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionToken,
            @PathVariable String id
    ) {
        ensureAdminAccess(sessionToken);
        return StandardSuccessResponse.ok(adminOperationsService.getPollAttempts(id));
    }

    @PostMapping("/generation-jobs/{id}/reconcile-now")
    public StandardSuccessResponse<GenerationJobResponse> reconcileNow(
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionToken,
            @PathVariable String id
    ) {
        ensureAdminAccess(sessionToken);
        return StandardSuccessResponse.ok(adminOperationsService.reconcileNow(id));
    }

    @PostMapping("/generation-jobs/{id}/retry-submit")
    public StandardSuccessResponse<GenerationJobResponse> retrySubmit(
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionToken,
            @PathVariable String id
    ) {
        ensureAdminAccess(sessionToken);
        return StandardSuccessResponse.ok(adminOperationsService.retrySubmit(id));
    }

    @PostMapping("/generation-jobs/{id}/mark-expired")
    public StandardSuccessResponse<ManualActionResponse> markExpired(
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionToken,
            @PathVariable String id
    ) {
        ensureAdminAccess(sessionToken);
        return StandardSuccessResponse.ok(adminOperationsService.markExpired(id));
    }

    @GetMapping("/schedules")
    public StandardSuccessResponse<List<ScheduleDefinitionResponse>> schedules(
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionToken
    ) {
        ensureAdminAccess(sessionToken);
        return StandardSuccessResponse.ok(adminOperationsService.getSchedules());
    }

    @GetMapping("/schedules/{id}")
    public StandardSuccessResponse<ScheduleDefinitionResponse> schedule(
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionToken,
            @PathVariable String id
    ) {
        ensureAdminAccess(sessionToken);
        return StandardSuccessResponse.ok(adminOperationsService.getSchedule(id));
    }

    @GetMapping("/schedules/{id}/runs")
    public StandardSuccessResponse<List<ScheduleRunResponse>> scheduleRuns(
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionToken,
            @PathVariable String id
    ) {
        ensureAdminAccess(sessionToken);
        return StandardSuccessResponse.ok(adminOperationsService.getScheduleRuns(id));
    }

    @GetMapping("/users")
    public StandardSuccessResponse<List<AdminUserSummaryResponse>> users(
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionToken,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Boolean frozenOnly
    ) {
        ensureAdminAccess(sessionToken);
        return StandardSuccessResponse.ok(adminOperationsService.getUsers(email, frozenOnly));
    }

    @GetMapping("/users/{id}")
    public StandardSuccessResponse<AdminUserDetailResponse> user(
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionToken,
            @PathVariable String id
    ) {
        ensureAdminAccess(sessionToken);
        return StandardSuccessResponse.ok(adminOperationsService.getUser(id));
    }

    @GetMapping("/users/{id}/security-logs")
    public StandardSuccessResponse<List<SecurityLogResponse>> userSecurityLogs(
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionToken,
            @PathVariable String id
    ) {
        ensureAdminAccess(sessionToken);
        return StandardSuccessResponse.ok(adminOperationsService.getUserSecurityLogs(id));
    }

    @PostMapping("/users/{id}/freeze")
    public StandardSuccessResponse<ManualActionResponse> freezeUser(
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionToken,
            @PathVariable String id,
            @Valid @RequestBody FreezeUserRequest request
    ) {
        String adminUserId = ensureAdminAccess(sessionToken);
        return StandardSuccessResponse.ok(adminOperationsService.freezeUser(adminUserId, id, request.reason()));
    }

    @PostMapping("/users/{id}/unfreeze")
    public StandardSuccessResponse<ManualActionResponse> unfreezeUser(
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionToken,
            @PathVariable String id
    ) {
        String adminUserId = ensureAdminAccess(sessionToken);
        return StandardSuccessResponse.ok(adminOperationsService.unfreezeUser(adminUserId, id));
    }

    @GetMapping("/lyrics")
    public StandardSuccessResponse<List<AdminLyricSummaryResponse>> lyrics(
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionToken,
            @RequestParam(required = false) String keyword
    ) {
        ensureAdminAccess(sessionToken);
        return StandardSuccessResponse.ok(adminOperationsService.getLyrics(keyword));
    }

    @GetMapping("/lyrics/{id}")
    public StandardSuccessResponse<AdminLyricDetailResponse> lyric(
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionToken,
            @PathVariable String id
    ) {
        ensureAdminAccess(sessionToken);
        return StandardSuccessResponse.ok(adminOperationsService.getLyric(id));
    }

    @GetMapping("/songs")
    public StandardSuccessResponse<List<AdminSongSummaryResponse>> songs(
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionToken,
            @RequestParam(required = false) String keyword
    ) {
        ensureAdminAccess(sessionToken);
        return StandardSuccessResponse.ok(adminOperationsService.getSongs(keyword));
    }

    @GetMapping("/songs/{id}")
    public StandardSuccessResponse<AdminSongDetailResponse> song(
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionToken,
            @PathVariable String id
    ) {
        ensureAdminAccess(sessionToken);
        return StandardSuccessResponse.ok(adminOperationsService.getSong(id));
    }

    @GetMapping("/invite-codes")
    public StandardSuccessResponse<List<AdminInviteCodeResponse>> inviteCodes(
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionToken,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean used
    ) {
        ensureAdminAccess(sessionToken);
        return StandardSuccessResponse.ok(adminOperationsService.getInviteCodes(keyword, active, used));
    }

    @PostMapping("/invite-codes/generate")
    public StandardSuccessResponse<List<AdminInviteCodeResponse>> generateInviteCodes(
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionToken,
            @Valid @RequestBody GenerateInviteCodesRequest request
    ) {
        ensureAdminAccess(sessionToken);
        return StandardSuccessResponse.ok(adminOperationsService.generateInviteCodes(request.count()));
    }

    @PostMapping("/invite-codes/{id}/activate")
    public StandardSuccessResponse<ManualActionResponse> activateInviteCode(
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionToken,
            @PathVariable String id
    ) {
        ensureAdminAccess(sessionToken);
        return StandardSuccessResponse.ok(adminOperationsService.activateInviteCode(id));
    }

    @PostMapping("/invite-codes/{id}/deactivate")
    public StandardSuccessResponse<ManualActionResponse> deactivateInviteCode(
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionToken,
            @PathVariable String id
    ) {
        ensureAdminAccess(sessionToken);
        return StandardSuccessResponse.ok(adminOperationsService.deactivateInviteCode(id));
    }

    @PostMapping("/schedules/{id}/run-now")
    public StandardSuccessResponse<ScheduleRunResponse> runNow(
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionToken,
            @PathVariable String id
    ) {
        ensureAdminAccess(sessionToken);
        return StandardSuccessResponse.ok(scheduleService.runNow(id));
    }

    @GetMapping("/health/summary")
    public StandardSuccessResponse<AdminHealthSummaryResponse> healthSummary(
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionToken
    ) {
        ensureAdminAccess(sessionToken);
        return StandardSuccessResponse.ok(adminOperationsService.getHealthSummary());
    }

    private String ensureAdminAccess(String sessionToken) {
        if (!featureFlagsProperties.isAdminEndpointsEnabled()) {
            throw new BadRequestException("Admin endpoints are disabled by configuration");
        }
        return authService.requireAdminUser(sessionToken).getId();
    }
}
