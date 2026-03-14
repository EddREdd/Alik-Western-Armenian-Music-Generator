package com.balians.musicgen.admin.service;

import com.balians.musicgen.admin.dto.AdminDashboardResponse;
import com.balians.musicgen.admin.dto.AdminGenerationJobFilter;
import com.balians.musicgen.admin.dto.AdminGenerationJobSummaryResponse;
import com.balians.musicgen.admin.dto.AdminHealthSummaryResponse;
import com.balians.musicgen.admin.dto.AdminInviteCodeResponse;
import com.balians.musicgen.admin.dto.AdminLyricDetailResponse;
import com.balians.musicgen.admin.dto.AdminLyricSummaryResponse;
import com.balians.musicgen.admin.dto.AdminMetricSnapshotResponse;
import com.balians.musicgen.admin.dto.AdminSongDetailResponse;
import com.balians.musicgen.admin.dto.AdminSongSummaryResponse;
import com.balians.musicgen.admin.dto.AdminUserDetailResponse;
import com.balians.musicgen.admin.dto.AdminUserSummaryResponse;
import com.balians.musicgen.admin.dto.CallbackEventSummaryResponse;
import com.balians.musicgen.admin.dto.ManualActionResponse;
import com.balians.musicgen.admin.dto.SecurityLogResponse;
import com.balians.musicgen.auth.model.InviteCode;
import com.balians.musicgen.auth.model.SecurityLog;
import com.balians.musicgen.auth.model.UserAccount;
import com.balians.musicgen.auth.repository.InviteCodeRepository;
import com.balians.musicgen.auth.repository.SecurityLogRepository;
import com.balians.musicgen.auth.repository.UserAccountRepository;
import com.balians.musicgen.auth.repository.UserSessionRepository;
import com.balians.musicgen.auth.service.SecurityLogService;
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
import com.balians.musicgen.lyrics.model.LyricEntry;
import com.balians.musicgen.lyrics.repository.LyricEntryRepository;
import com.balians.musicgen.polling.dto.PollAttemptResponse;
import com.balians.musicgen.polling.repository.PollAttemptRepository;
import com.balians.musicgen.polling.service.PollingReconciliationService;
import com.balians.musicgen.schedule.dto.ScheduleDefinitionResponse;
import com.balians.musicgen.schedule.dto.ScheduleRunResponse;
import com.balians.musicgen.schedule.repository.ScheduleDefinitionRepository;
import com.balians.musicgen.schedule.repository.ScheduleRunRepository;
import com.balians.musicgen.schedule.service.ScheduleService;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOperationsService {

    private static final String INVITE_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final GenerationJobRepository generationJobRepository;
    private final GenerationTrackRepository generationTrackRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserSessionRepository userSessionRepository;
    private final InviteCodeRepository inviteCodeRepository;
    private final SecurityLogRepository securityLogRepository;
    private final SecurityLogService securityLogService;
    private final LyricEntryRepository lyricEntryRepository;
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
        return new PageImpl<>(items.subList(start, end), PageRequest.of(safePage, safeSize), items.size());
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

    public List<AdminUserSummaryResponse> getUsers(String emailQuery, Boolean frozenOnly) {
        Map<String, InviteCode> inviteByUserId = inviteCodeRepository.findAll().stream()
                .filter(invite -> hasText(invite.getUsedByUserId()))
                .collect(Collectors.toMap(InviteCode::getUsedByUserId, invite -> invite, (left, right) -> left));

        return userAccountRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(user -> matchesUser(user, emailQuery, frozenOnly))
                .map(user -> mapUserSummary(user, inviteByUserId.get(user.getId())))
                .toList();
    }

    public AdminUserDetailResponse getUser(String id) {
        UserAccount user = getUserEntity(id);
        InviteCode inviteCode = inviteCodeRepository.findAll().stream()
                .filter(invite -> id.equals(invite.getUsedByUserId()))
                .findFirst()
                .orElse(null);
        return mapUserDetail(user, inviteCode);
    }

    public List<SecurityLogResponse> getUserSecurityLogs(String userId) {
        getUserEntity(userId);
        return securityLogRepository.findByUserIdOrderByOccurredAtDesc(userId)
                .stream()
                .map(this::mapSecurityLog)
                .toList();
    }

    public ManualActionResponse freezeUser(String adminUserId, String userId, String reason) {
        UserAccount user = getUserEntity(userId);
        if (adminUserId.equals(userId)) {
            throw new ConflictException("You cannot freeze your own account");
        }
        if (Boolean.TRUE.equals(user.getFrozen())) {
            throw new ConflictException("User account is already frozen");
        }

        user.setFrozen(true);
        user.setFrozenAt(Instant.now());
        user.setFreezeReason(reason.trim());
        userAccountRepository.save(user);
        userSessionRepository.deleteByUserId(userId);
        securityLogService.log(user.getId(), user.getEmail(), "ACCOUNT_FROZEN", "Frozen by admin: " + reason.trim());
        log.warn("Froze user account id={} email={}", user.getId(), user.getEmail());
        return new ManualActionResponse("freeze-user", userId, "OK", "User account frozen");
    }

    public ManualActionResponse unfreezeUser(String adminUserId, String userId) {
        if (adminUserId.equals(userId)) {
            throw new ConflictException("You cannot unfreeze your own account through this action");
        }
        UserAccount user = getUserEntity(userId);
        if (!Boolean.TRUE.equals(user.getFrozen())) {
            throw new ConflictException("User account is not frozen");
        }

        user.setFrozen(false);
        user.setFrozenAt(null);
        user.setFreezeReason(null);
        userAccountRepository.save(user);
        securityLogService.log(user.getId(), user.getEmail(), "ACCOUNT_UNFROZEN", "Unfrozen by admin");
        log.info("Unfroze user account id={} email={}", user.getId(), user.getEmail());
        return new ManualActionResponse("unfreeze-user", userId, "OK", "User account unfrozen");
    }

    public List<AdminLyricSummaryResponse> getLyrics(String keyword) {
        return lyricEntryRepository.findAll()
                .stream()
                .filter(lyric -> matchesLyric(lyric, keyword))
                .sorted(Comparator.comparing(LyricEntry::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(this::mapLyricSummary)
                .toList();
    }

    public AdminLyricDetailResponse getLyric(String id) {
        LyricEntry lyric = lyricEntryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Lyric not found: " + id));
        return mapLyricDetail(lyric);
    }

    public List<AdminSongSummaryResponse> getSongs(String keyword) {
        return generationTrackRepository.findAll()
                .stream()
                .filter(track -> matchesSong(track, keyword))
                .sorted(Comparator.comparing(GenerationTrack::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(this::mapSongSummary)
                .toList();
    }

    public AdminSongDetailResponse getSong(String id) {
        GenerationTrack track = generationTrackRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Song not found: " + id));
        return mapSongDetail(track);
    }

    public List<AdminInviteCodeResponse> getInviteCodes(String keyword, Boolean active, Boolean used) {
        Map<String, UserAccount> usersById = userAccountRepository.findAll().stream()
                .collect(Collectors.toMap(UserAccount::getId, user -> user, (left, right) -> left));

        return inviteCodeRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(invite -> matchesInvite(invite, keyword, active, used, usersById))
                .map(invite -> mapInviteCode(invite, usersById.get(invite.getUsedByUserId())))
                .toList();
    }

    public List<AdminInviteCodeResponse> generateInviteCodes(int count) {
        if (count < 1 || count > 100) {
            throw new BadRequestException("Invite code generation count must be between 1 and 100");
        }

        Set<String> existingCodes = inviteCodeRepository.findAll().stream()
                .map(InviteCode::getCode)
                .collect(Collectors.toSet());

        List<InviteCode> created = java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> createUniqueInviteCode(existingCodes))
                .toList();
        inviteCodeRepository.saveAll(created);
        log.info("Generated {} invite code(s)", created.size());
        return created.stream()
                .sorted(Comparator.comparing(InviteCode::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(invite -> mapInviteCode(invite, null))
                .toList();
    }

    public ManualActionResponse activateInviteCode(String id) {
        InviteCode inviteCode = getInviteCode(id);
        if (hasText(inviteCode.getUsedByUserId())) {
            throw new ConflictException("Used invite codes cannot be reactivated");
        }
        inviteCode.setActive(true);
        inviteCodeRepository.save(inviteCode);
        return new ManualActionResponse("activate-invite", id, "OK", "Invite code activated");
    }

    public ManualActionResponse deactivateInviteCode(String id) {
        InviteCode inviteCode = getInviteCode(id);
        inviteCode.setActive(false);
        inviteCodeRepository.save(inviteCode);
        return new ManualActionResponse("deactivate-invite", id, "OK", "Invite code deactivated");
    }

    public AdminDashboardResponse getDashboard() {
        Instant now = Instant.now();
        List<GenerationTrack> songs = generationTrackRepository.findAll();
        List<UserAccount> users = userAccountRepository.findAll();
        List<GenerationJob> submittedJobs = generationJobRepository.findAll().stream()
                .filter(job -> job.getSubmittedAt() != null)
                .toList();
        List<InviteCode> inviteCodes = inviteCodeRepository.findAll();

        return new AdminDashboardResponse(
                buildSnapshot(songs.stream().map(GenerationTrack::getCreatedAt).toList(), now),
                buildSnapshot(users.stream().map(UserAccount::getCreatedAt).toList(), now),
                buildSnapshot(submittedJobs.stream().map(GenerationJob::getSubmittedAt).toList(), now),
                users.size(),
                users.stream().filter(user -> Boolean.TRUE.equals(user.getFrozen())).count(),
                inviteCodes.stream().filter(invite -> Boolean.TRUE.equals(invite.getActive()) && !hasText(invite.getUsedByUserId())).count(),
                inviteCodes.stream().filter(invite -> hasText(invite.getUsedByUserId())).count(),
                lyricEntryRepository.count(),
                songs.size()
        );
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
                userAccountRepository.count(),
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

    private UserAccount getUserEntity(String id) {
        return userAccountRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
    }

    private InviteCode getInviteCode(String id) {
        return inviteCodeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Invite code not found: " + id));
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
        return !Boolean.TRUE.equals(filter.stuckOnly()) || isStuck(job);
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
        Instant reference = job.getLastPolledAt() != null ? job.getLastPolledAt()
                : job.getSubmittedAt() != null ? job.getSubmittedAt()
                : job.getCreatedAt();
        if (reference == null) {
            return false;
        }
        return reference.isBefore(Instant.now().minusSeconds(opsProperties.getStuckThresholdMinutes() * 60L));
    }

    private boolean matchesUser(UserAccount user, String emailQuery, Boolean frozenOnly) {
        if (hasText(emailQuery) && !containsIgnoreCase(user.getEmail(), emailQuery)) {
            return false;
        }
        return !Boolean.TRUE.equals(frozenOnly) || Boolean.TRUE.equals(user.getFrozen());
    }

    private boolean matchesLyric(LyricEntry lyric, String keyword) {
        if (!hasText(keyword)) {
            return true;
        }
        return containsIgnoreCase(lyric.getTitle(), keyword)
                || containsIgnoreCase(lyric.getBody(), keyword)
                || containsIgnoreCase(lyric.getUserId(), keyword)
                || containsIgnoreCase(lyric.getProjectId(), keyword);
    }

    private boolean matchesSong(GenerationTrack track, String keyword) {
        if (!hasText(keyword)) {
            return true;
        }
        return containsIgnoreCase(track.getTitle(), keyword)
                || containsIgnoreCase(track.getTags() == null ? null : String.join(" ", track.getTags()), keyword)
                || containsIgnoreCase(track.getLyricTitle(), keyword)
                || containsIgnoreCase(track.getLyricText(), keyword)
                || containsIgnoreCase(track.getOwnerUserId(), keyword)
                || containsIgnoreCase(track.getProjectId(), keyword);
    }

    private boolean matchesInvite(
            InviteCode invite,
            String keyword,
            Boolean active,
            Boolean used,
            Map<String, UserAccount> usersById
    ) {
        if (active != null && active != invite.getActive()) {
            return false;
        }
        boolean isUsed = hasText(invite.getUsedByUserId());
        if (used != null && used != isUsed) {
            return false;
        }
        if (!hasText(keyword)) {
            return true;
        }
        UserAccount usedBy = usersById.get(invite.getUsedByUserId());
        return containsIgnoreCase(invite.getCode(), keyword)
                || containsIgnoreCase(invite.getUsedByUserId(), keyword)
                || containsIgnoreCase(usedBy == null ? null : usedBy.getEmail(), keyword);
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
                track.getLocalAudioUrl(),
                track.getLocalImageUrl(),
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

    private AdminUserSummaryResponse mapUserSummary(UserAccount user, InviteCode inviteCode) {
        return new AdminUserSummaryResponse(
                user.getId(),
                user.getEmail(),
                user.getCreatedAt(),
                user.getSongsGenerated(),
                user.getCreditsRemaining(),
                inviteCode == null ? null : inviteCode.getCode(),
                Boolean.TRUE.equals(user.getFrozen()),
                Boolean.TRUE.equals(user.getAdmin()),
                Boolean.TRUE.equals(user.getUnlimitedCredits())
        );
    }

    private AdminUserDetailResponse mapUserDetail(UserAccount user, InviteCode inviteCode) {
        return new AdminUserDetailResponse(
                user.getId(),
                user.getEmail(),
                user.getCreatedAt(),
                user.getSongsGenerated(),
                user.getCreditsRemaining(),
                user.getCreditsUsed(),
                user.getSongsGenerated(),
                Boolean.TRUE.equals(user.getEmailVerified()),
                inviteCode == null ? null : inviteCode.getCode(),
                Boolean.TRUE.equals(user.getFrozen()),
                user.getFrozenAt(),
                user.getFreezeReason(),
                Boolean.TRUE.equals(user.getAdmin()),
                Boolean.TRUE.equals(user.getUnlimitedCredits()),
                user.getGoogleEmail()
        );
    }

    private SecurityLogResponse mapSecurityLog(SecurityLog logEntry) {
        return new SecurityLogResponse(
                logEntry.getId(),
                logEntry.getUserId(),
                logEntry.getEmail(),
                logEntry.getEventType(),
                logEntry.getDetails(),
                logEntry.getOccurredAt()
        );
    }

    private AdminLyricSummaryResponse mapLyricSummary(LyricEntry lyric) {
        return new AdminLyricSummaryResponse(
                lyric.getId(),
                lyric.getUserId(),
                lyric.getProjectId(),
                lyric.getTitle(),
                preview(lyric.getBody(), 120),
                lyric.getCurrentVersion(),
                Boolean.TRUE.equals(lyric.getLocked()),
                lyric.getLinkedSongIds(),
                lyric.getUpdatedAt()
        );
    }

    private AdminLyricDetailResponse mapLyricDetail(LyricEntry lyric) {
        return new AdminLyricDetailResponse(
                lyric.getId(),
                lyric.getUserId(),
                lyric.getProjectId(),
                lyric.getTitle(),
                lyric.getBody(),
                lyric.getCurrentVersion(),
                Boolean.TRUE.equals(lyric.getLocked()),
                lyric.getLinkedSongIds(),
                lyric.getCreatedAt(),
                lyric.getUpdatedAt()
        );
    }

    private AdminSongSummaryResponse mapSongSummary(GenerationTrack track) {
        return new AdminSongSummaryResponse(
                track.getId(),
                track.getGenerationJobId(),
                track.getOwnerUserId(),
                track.getProjectId(),
                track.getTitle(),
                track.getAudioUrl(),
                track.getStreamAudioUrl(),
                track.getLyricId(),
                track.getLyricTitle(),
                track.getCreatedAt(),
                track.getTags()
        );
    }

    private AdminSongDetailResponse mapSongDetail(GenerationTrack track) {
        return new AdminSongDetailResponse(
                track.getId(),
                track.getGenerationJobId(),
                track.getOwnerUserId(),
                track.getProjectId(),
                track.getProviderMusicId(),
                track.getTitle(),
                track.getAudioUrl(),
                track.getStreamAudioUrl(),
                track.getImageUrl(),
                track.getLyricId(),
                track.getLyricTitle(),
                track.getLyricText(),
                track.getModelName(),
                track.getTags(),
                track.getDurationSeconds(),
                track.getProviderCreateTime(),
                track.getCreatedAt()
        );
    }

    private AdminInviteCodeResponse mapInviteCode(InviteCode inviteCode, UserAccount usedBy) {
        return new AdminInviteCodeResponse(
                inviteCode.getId(),
                inviteCode.getCode(),
                Boolean.TRUE.equals(inviteCode.getActive()),
                inviteCode.getUsedByUserId(),
                usedBy == null ? null : usedBy.getEmail(),
                inviteCode.getUsedAt(),
                inviteCode.getCreatedAt()
        );
    }

    private AdminMetricSnapshotResponse buildSnapshot(List<Instant> timestamps, Instant now) {
        Instant dailyCutoff = now.minus(Duration.ofDays(1));
        Instant weeklyCutoff = now.minus(Duration.ofDays(7));
        Instant monthlyCutoff = now.minus(Duration.ofDays(30));

        return new AdminMetricSnapshotResponse(
                timestamps.stream().filter(timestamp -> timestamp != null && !timestamp.isBefore(dailyCutoff)).count(),
                timestamps.stream().filter(timestamp -> timestamp != null && !timestamp.isBefore(weeklyCutoff)).count(),
                timestamps.stream().filter(timestamp -> timestamp != null && !timestamp.isBefore(monthlyCutoff)).count(),
                timestamps.stream().filter(java.util.Objects::nonNull).count()
        );
    }

    private InviteCode createUniqueInviteCode(Set<String> existingCodes) {
        SecureRandom random = new SecureRandom();
        String code;
        do {
            StringBuilder builder = new StringBuilder("ALIK-");
            for (int index = 0; index < 8; index++) {
                builder.append(INVITE_CODE_ALPHABET.charAt(random.nextInt(INVITE_CODE_ALPHABET.length())));
            }
            code = builder.toString();
        } while (!existingCodes.add(code));

        return InviteCode.builder()
                .code(code)
                .active(true)
                .build();
    }

    private boolean containsIgnoreCase(String source, String search) {
        return source != null && search != null && source.toLowerCase().contains(search.trim().toLowerCase());
    }

    private String preview(String value, int maxLength) {
        if (!hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength) + "...";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
