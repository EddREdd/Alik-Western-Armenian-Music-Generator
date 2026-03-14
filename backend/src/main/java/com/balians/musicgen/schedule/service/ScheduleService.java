package com.balians.musicgen.schedule.service;

import com.balians.musicgen.common.enums.JobSourceType;
import com.balians.musicgen.common.enums.ScheduleRunStatus;
import com.balians.musicgen.common.exception.BadRequestException;
import com.balians.musicgen.common.exception.NotFoundException;
import com.balians.musicgen.generation.dto.CreateGenerationJobRequest;
import com.balians.musicgen.generation.dto.GenerationJobResponse;
import com.balians.musicgen.generation.service.GenerationJobService;
import com.balians.musicgen.generation.service.GenerationSubmissionService;
import com.balians.musicgen.prompttemplate.model.PromptTemplate;
import com.balians.musicgen.prompttemplate.repository.PromptTemplateRepository;
import com.balians.musicgen.provider.client.SunoClient;
import com.balians.musicgen.provider.dto.SunoCreditResponse;
import com.balians.musicgen.schedule.dto.CreateScheduleRequest;
import com.balians.musicgen.schedule.dto.ScheduleDefinitionResponse;
import com.balians.musicgen.schedule.dto.ScheduleRunResponse;
import com.balians.musicgen.schedule.dto.UpdateScheduleRequest;
import com.balians.musicgen.schedule.model.ScheduleDefinition;
import com.balians.musicgen.schedule.model.ScheduleRun;
import com.balians.musicgen.schedule.repository.ScheduleDefinitionRepository;
import com.balians.musicgen.schedule.repository.ScheduleRunRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleDefinitionRepository scheduleDefinitionRepository;
    private final ScheduleRunRepository scheduleRunRepository;
    private final PromptTemplateRepository promptTemplateRepository;
    private final GenerationJobService generationJobService;
    private final GenerationSubmissionService generationSubmissionService;
    private final SunoClient sunoClient;
    private final ScheduleExecutionProperties scheduleExecutionProperties;

    public ScheduleDefinitionResponse createSchedule(CreateScheduleRequest request) {
        ZoneId zoneId = validateZone(request.timezone());
        CronExpression cronExpression = validateCron(request.cronExpression());

        ScheduleDefinition schedule = ScheduleDefinition.builder()
                .projectId(request.projectId().trim())
                .templateId(request.templateId().trim())
                .name(request.name().trim())
                .timezone(zoneId.getId())
                .cronExpression(cronExpression.toString())
                .enabled(request.enabled())
                .autoSubmitToProvider(request.autoSubmitToProvider())
                .creditsMinThreshold(defaultCreditsThreshold(request.creditsMinThreshold()))
                .nextRunAt(Boolean.TRUE.equals(request.enabled()) ? calculateNextRunAt(cronExpression, zoneId, Instant.now()) : null)
                .build();

        return map(scheduleDefinitionRepository.save(schedule));
    }

    public ScheduleDefinitionResponse updateSchedule(String id, UpdateScheduleRequest request) {
        ScheduleDefinition schedule = getScheduleEntity(id);
        ZoneId zoneId = validateZone(request.timezone());
        CronExpression cronExpression = validateCron(request.cronExpression());

        schedule.setTemplateId(request.templateId().trim());
        schedule.setName(request.name().trim());
        schedule.setTimezone(zoneId.getId());
        schedule.setCronExpression(cronExpression.toString());
        schedule.setEnabled(request.enabled());
        schedule.setAutoSubmitToProvider(request.autoSubmitToProvider());
        schedule.setCreditsMinThreshold(defaultCreditsThreshold(request.creditsMinThreshold()));
        schedule.setNextRunAt(Boolean.TRUE.equals(request.enabled()) ? calculateNextRunAt(cronExpression, zoneId, Instant.now()) : null);

        return map(scheduleDefinitionRepository.save(schedule));
    }

    public ScheduleDefinitionResponse getSchedule(String id) {
        return map(getScheduleEntity(id));
    }

    public List<ScheduleDefinitionResponse> listSchedules() {
        return scheduleDefinitionRepository.findAll()
                .stream()
                .sorted((left, right) -> {
                    Instant l = left.getNextRunAt();
                    Instant r = right.getNextRunAt();
                    if (l == null && r == null) {
                        return 0;
                    }
                    if (l == null) {
                        return 1;
                    }
                    if (r == null) {
                        return -1;
                    }
                    return l.compareTo(r);
                })
                .map(this::map)
                .toList();
    }

    public List<ScheduleDefinitionResponse> listDueSchedules() {
        return findDueSchedules()
                .stream()
                .map(this::map)
                .toList();
    }

    public ScheduleDefinitionResponse enableSchedule(String id) {
        ScheduleDefinition schedule = getScheduleEntity(id);
        schedule.setEnabled(Boolean.TRUE);
        schedule.setNextRunAt(calculateNextRunAt(validateCron(schedule.getCronExpression()), validateZone(schedule.getTimezone()), Instant.now()));
        return map(scheduleDefinitionRepository.save(schedule));
    }

    public ScheduleDefinitionResponse disableSchedule(String id) {
        ScheduleDefinition schedule = getScheduleEntity(id);
        schedule.setEnabled(Boolean.FALSE);
        schedule.setNextRunAt(null);
        return map(scheduleDefinitionRepository.save(schedule));
    }

    public List<ScheduleRunResponse> getScheduleRuns(String scheduleId) {
        getScheduleEntity(scheduleId);
        return scheduleRunRepository.findByScheduleDefinitionIdOrderByStartedAtDesc(scheduleId)
                .stream()
                .map(this::mapRun)
                .toList();
    }

    public ScheduleRunResponse runNow(String scheduleId) {
        ScheduleDefinition schedule = getScheduleEntity(scheduleId);
        return mapRun(executeSchedule(schedule, true));
    }

    public void executeDueSchedules() {
        List<ScheduleDefinition> dueSchedules = findDueSchedules();
        log.info("Schedule scan started; found {} due schedule(s)", dueSchedules.size());
        dueSchedules.stream()
                .limit(scheduleExecutionProperties.getBatchSize())
                .forEach(schedule -> executeSchedule(schedule, false));
    }

    private List<ScheduleDefinition> findDueSchedules() {
        return scheduleDefinitionRepository.findByEnabledTrueAndNextRunAtLessThanEqualOrderByNextRunAtAsc(Instant.now());
    }

    private ScheduleRun executeSchedule(ScheduleDefinition schedule, boolean manualOverride) {
        ZoneId zoneId = validateZone(schedule.getTimezone());
        CronExpression cronExpression = validateCron(schedule.getCronExpression());
        LocalDate runDate = ZonedDateTime.now(zoneId).toLocalDate();

        if (!manualOverride && !Boolean.TRUE.equals(schedule.getEnabled())) {
            return createSkippedRun(schedule, runDate, "Schedule is disabled", null);
        }

        if (hasDuplicateRun(schedule.getId(), runDate, manualOverride)) {
            return createSkippedRun(schedule, runDate, "Duplicate daily run prevented", null);
        }

        Integer creditsSnapshot = null;
        try {
            creditsSnapshot = fetchCredits();
            if (creditsSnapshot != null && creditsSnapshot < defaultCreditsThreshold(schedule.getCreditsMinThreshold())) {
                return createSkippedRun(schedule, runDate, "Insufficient credits", creditsSnapshot);
            }
        } catch (Exception ex) {
            log.warn("Credit check failed for schedule id={}", schedule.getId(), ex);
            return createFailedRun(schedule, runDate, "Credit check failed: " + ex.getMessage(), creditsSnapshot);
        }

        PromptTemplate template = promptTemplateRepository.findById(schedule.getTemplateId())
                .orElseThrow(() -> new NotFoundException("Prompt template not found for schedule: " + schedule.getTemplateId()));

        ScheduleRun run = scheduleRunRepository.save(ScheduleRun.builder()
                .scheduleDefinitionId(schedule.getId())
                .runDate(runDate)
                .status(ScheduleRunStatus.STARTED)
                .startedAt(Instant.now())
                .creditsSnapshot(creditsSnapshot)
                .providerSubmittedFlag(Boolean.FALSE)
                .build());

        try {
            GenerationJobResponse createdJob = generationJobService.createJob(new CreateGenerationJobRequest(
                    schedule.getProjectId(),
                    template.getId(),
                    null,
                    JobSourceType.SCHEDULED,
                    template.getPromptTemplate(),
                    template.getStyleTemplate(),
                    template.getTitleTemplate(),
                    template.getCustomMode(),
                    template.getInstrumental(),
                    template.getModel()
            ));

            run.setGenerationJobId(createdJob.id());
            run.setStatus(ScheduleRunStatus.CREATED);

            if (Boolean.TRUE.equals(schedule.getAutoSubmitToProvider())) {
                generationSubmissionService.submitJob(createdJob.id());
                run.setStatus(ScheduleRunStatus.SUBMITTED);
                run.setProviderSubmittedFlag(Boolean.TRUE);
            }

            run.setFinishedAt(Instant.now());
            scheduleRunRepository.save(run);

            schedule.setLastRunAt(run.getFinishedAt());
            schedule.setNextRunAt(calculateNextRunAt(cronExpression, zoneId, Instant.now()));
            scheduleDefinitionRepository.save(schedule);

            log.info("Executed schedule id={} runId={} jobId={} autoSubmitted={}",
                    schedule.getId(), run.getId(), createdJob.id(), schedule.getAutoSubmitToProvider());
            return run;
        } catch (Exception ex) {
            run.setStatus(ScheduleRunStatus.FAILED);
            run.setErrorMessage(ex.getMessage());
            run.setFinishedAt(Instant.now());
            scheduleRunRepository.save(run);

            schedule.setLastRunAt(run.getFinishedAt());
            schedule.setNextRunAt(calculateNextRunAt(cronExpression, zoneId, Instant.now()));
            scheduleDefinitionRepository.save(schedule);

            log.error("Schedule execution failed id={} runId={}", schedule.getId(), run.getId(), ex);
            return run;
        }
    }

    private boolean hasDuplicateRun(String scheduleId, LocalDate runDate, boolean manualOverride) {
        return scheduleRunRepository.findFirstByScheduleDefinitionIdAndRunDateOrderByStartedAtDesc(scheduleId, runDate)
                .filter(run -> run.getStatus() != ScheduleRunStatus.FAILED && run.getStatus() != ScheduleRunStatus.SKIPPED)
                .isPresent();
    }

    private ScheduleRun createSkippedRun(ScheduleDefinition schedule, LocalDate runDate, String reason, Integer creditsSnapshot) {
        ScheduleRun run = scheduleRunRepository.save(ScheduleRun.builder()
                .scheduleDefinitionId(schedule.getId())
                .runDate(runDate)
                .status(ScheduleRunStatus.SKIPPED)
                .startedAt(Instant.now())
                .finishedAt(Instant.now())
                .errorMessage(reason)
                .creditsSnapshot(creditsSnapshot)
                .providerSubmittedFlag(Boolean.FALSE)
                .build());
        log.info("Skipped schedule id={} reason={}", schedule.getId(), reason);
        return run;
    }

    private ScheduleRun createFailedRun(ScheduleDefinition schedule, LocalDate runDate, String reason, Integer creditsSnapshot) {
        ScheduleRun run = scheduleRunRepository.save(ScheduleRun.builder()
                .scheduleDefinitionId(schedule.getId())
                .runDate(runDate)
                .status(ScheduleRunStatus.FAILED)
                .startedAt(Instant.now())
                .finishedAt(Instant.now())
                .errorMessage(reason)
                .creditsSnapshot(creditsSnapshot)
                .providerSubmittedFlag(Boolean.FALSE)
                .build());
        log.warn("Failed schedule id={} reason={}", schedule.getId(), reason);
        return run;
    }

    private Integer fetchCredits() {
        SunoCreditResponse response = sunoClient.getCredits();
        if (response == null || response.code() == null || response.code() != 200) {
            throw new BadRequestException("Provider credit check returned an invalid response");
        }
        return response.data();
    }

    private ScheduleDefinitionResponse map(ScheduleDefinition schedule) {
        return new ScheduleDefinitionResponse(
                schedule.getId(),
                schedule.getProjectId(),
                schedule.getTemplateId(),
                schedule.getName(),
                schedule.getTimezone(),
                schedule.getCronExpression(),
                schedule.getEnabled(),
                schedule.getAutoSubmitToProvider(),
                schedule.getCreditsMinThreshold(),
                schedule.getLastRunAt(),
                schedule.getNextRunAt(),
                schedule.getCreatedAt(),
                schedule.getUpdatedAt()
        );
    }

    private ScheduleRunResponse mapRun(ScheduleRun run) {
        return new ScheduleRunResponse(
                run.getId(),
                run.getScheduleDefinitionId(),
                run.getGenerationJobId(),
                run.getRunDate(),
                run.getStatus(),
                run.getStartedAt(),
                run.getFinishedAt(),
                run.getErrorMessage(),
                run.getCreditsSnapshot(),
                run.getProviderSubmittedFlag()
        );
    }

    private ScheduleDefinition getScheduleEntity(String id) {
        return scheduleDefinitionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Schedule not found: " + id));
    }

    private ZoneId validateZone(String timezone) {
        try {
            return ZoneId.of(timezone.trim());
        } catch (Exception ex) {
            throw new BadRequestException("Invalid timezone: " + timezone);
        }
    }

    private CronExpression validateCron(String cronExpression) {
        if (cronExpression == null || cronExpression.trim().isEmpty()) {
            throw new BadRequestException("cronExpression is required");
        }
        try {
            return CronExpression.parse(cronExpression.trim());
        } catch (Exception ex) {
            throw new BadRequestException("Invalid cronExpression");
        }
    }

    private Instant calculateNextRunAt(CronExpression cronExpression, ZoneId zoneId, Instant referenceTime) {
        ZonedDateTime zonedReference = ZonedDateTime.ofInstant(referenceTime, zoneId);
        ZonedDateTime next = cronExpression.next(zonedReference);
        if (next == null) {
            throw new BadRequestException("Unable to calculate nextRunAt from cronExpression");
        }
        return next.toInstant();
    }

    private Integer defaultCreditsThreshold(Integer value) {
        return value == null ? 0 : value;
    }
}
