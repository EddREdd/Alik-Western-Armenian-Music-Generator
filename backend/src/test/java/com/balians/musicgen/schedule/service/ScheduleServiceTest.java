package com.balians.musicgen.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.balians.musicgen.common.enums.GenerationModel;
import com.balians.musicgen.common.enums.ScheduleRunStatus;
import com.balians.musicgen.generation.dto.GenerationJobResponse;
import com.balians.musicgen.generation.service.GenerationJobService;
import com.balians.musicgen.generation.service.GenerationSubmissionService;
import com.balians.musicgen.prompttemplate.model.PromptTemplate;
import com.balians.musicgen.prompttemplate.repository.PromptTemplateRepository;
import com.balians.musicgen.provider.client.SunoClient;
import com.balians.musicgen.provider.dto.SunoCreditResponse;
import com.balians.musicgen.schedule.dto.CreateScheduleRequest;
import com.balians.musicgen.schedule.dto.ScheduleRunResponse;
import com.balians.musicgen.schedule.model.ScheduleDefinition;
import com.balians.musicgen.schedule.model.ScheduleRun;
import com.balians.musicgen.schedule.repository.ScheduleDefinitionRepository;
import com.balians.musicgen.schedule.repository.ScheduleRunRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock private ScheduleDefinitionRepository scheduleDefinitionRepository;
    @Mock private ScheduleRunRepository scheduleRunRepository;
    @Mock private PromptTemplateRepository promptTemplateRepository;
    @Mock private GenerationJobService generationJobService;
    @Mock private GenerationSubmissionService generationSubmissionService;
    @Mock private SunoClient sunoClient;

    @InjectMocks
    private ScheduleService scheduleService;

    @Test
    void runNow_createsGenerationJobFromTemplate() {
        ScheduleExecutionProperties properties = new ScheduleExecutionProperties();
        properties.setBatchSize(10);
        scheduleService = new ScheduleService(scheduleDefinitionRepository, scheduleRunRepository, promptTemplateRepository,
                generationJobService, generationSubmissionService, sunoClient, properties);

        ScheduleDefinition schedule = ScheduleDefinition.builder()
                .id("schedule-1").projectId("project-1").templateId("template-1").name("Daily")
                .timezone("UTC").cronExpression("0 0 9 * * *").enabled(true).autoSubmitToProvider(false).build();
        PromptTemplate template = PromptTemplate.builder()
                .id("template-1").projectId("project-1").promptTemplate("prompt").styleTemplate("style").titleTemplate("title")
                .customMode(true).instrumental(false).model(GenerationModel.V4).build();

        when(scheduleDefinitionRepository.findById("schedule-1")).thenReturn(Optional.of(schedule));
        when(scheduleRunRepository.findFirstByScheduleDefinitionIdAndRunDateOrderByStartedAtDesc(any(), any())).thenReturn(Optional.empty());
        when(sunoClient.getCredits()).thenReturn(new SunoCreditResponse(200, "ok", 10));
        when(promptTemplateRepository.findById("template-1")).thenReturn(Optional.of(template));
        when(scheduleRunRepository.save(any(ScheduleRun.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(generationJobService.createJob(any())).thenReturn(new GenerationJobResponse("job-1", "project-1", "template-1", null,
                null, null, null, "prompt", "style", "title", true, false, GenerationModel.V4,
                null, null, Instant.now(), Instant.now(), null, null, null, java.util.List.of(), java.util.List.of()));

        ScheduleRunResponse response = scheduleService.runNow("schedule-1");

        assertThat(response.generationJobId()).isEqualTo("job-1");
        assertThat(response.status()).isEqualTo(ScheduleRunStatus.CREATED);
        verify(generationSubmissionService, never()).submitJob(any());
    }

    @Test
    void runNow_insufficientCreditsSkipsSafely() {
        ScheduleExecutionProperties properties = new ScheduleExecutionProperties();
        scheduleService = new ScheduleService(scheduleDefinitionRepository, scheduleRunRepository, promptTemplateRepository,
                generationJobService, generationSubmissionService, sunoClient, properties);

        ScheduleDefinition schedule = ScheduleDefinition.builder()
                .id("schedule-1").projectId("project-1").templateId("template-1").name("Daily")
                .timezone("UTC").cronExpression("0 0 9 * * *").enabled(true).autoSubmitToProvider(true).creditsMinThreshold(5).build();

        when(scheduleDefinitionRepository.findById("schedule-1")).thenReturn(Optional.of(schedule));
        when(scheduleRunRepository.findFirstByScheduleDefinitionIdAndRunDateOrderByStartedAtDesc(any(), any())).thenReturn(Optional.empty());
        when(sunoClient.getCredits()).thenReturn(new SunoCreditResponse(200, "ok", 1));
        when(scheduleRunRepository.save(any(ScheduleRun.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ScheduleRunResponse response = scheduleService.runNow("schedule-1");

        assertThat(response.status()).isEqualTo(ScheduleRunStatus.SKIPPED);
        verify(generationJobService, never()).createJob(any());
    }
}
