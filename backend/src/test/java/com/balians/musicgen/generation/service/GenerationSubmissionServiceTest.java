package com.balians.musicgen.generation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.balians.musicgen.common.enums.GenerationModel;
import com.balians.musicgen.common.enums.InternalJobStatus;
import com.balians.musicgen.common.enums.JobSourceType;
import com.balians.musicgen.common.enums.ProviderJobStatus;
import com.balians.musicgen.common.exception.ProviderIntegrationException;
import com.balians.musicgen.config.FeatureFlagsProperties;
import com.balians.musicgen.generation.mapper.GenerationJobMapper;
import com.balians.musicgen.generation.model.GenerationJob;
import com.balians.musicgen.generation.repository.GenerationJobRepository;
import com.balians.musicgen.generation.repository.GenerationTrackRepository;
import com.balians.musicgen.provider.client.SunoClient;
import com.balians.musicgen.provider.config.ProviderProperties;
import com.balians.musicgen.provider.dto.SunoGenerateResponse;
import com.balians.musicgen.provider.dto.SunoGenerateResponseData;
import com.balians.musicgen.provider.service.ProviderSubmissionValidator;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class GenerationSubmissionServiceTest {

    @Mock
    private GenerationJobRepository generationJobRepository;
    @Mock
    private GenerationTrackRepository generationTrackRepository;
    @Mock
    private GenerationJobMapper generationJobMapper;
    @Mock
    private ProviderSubmissionValidator providerSubmissionValidator;
    @Mock
    private ProviderProperties providerProperties;
    @Mock
    private FeatureFlagsProperties featureFlagsProperties;
    @Mock
    private SunoClient sunoClient;

    @InjectMocks
    private GenerationSubmissionService service;

    @Test
    void submitJob_successStoresTaskIdAndSubmittedState() {
        GenerationJob job = baseJob();
        when(generationJobRepository.findById("job-1")).thenReturn(Optional.of(job));
        when(featureFlagsProperties.isProviderSubmissionEnabled()).thenReturn(true);
        when(providerProperties.getCallbackBaseUrl()).thenReturn("http://localhost:8080");
        when(sunoClient.submitGeneration(any())).thenReturn(new SunoGenerateResponse(200, "ok", new SunoGenerateResponseData("task-123")));
        when(generationJobRepository.save(any(GenerationJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.submitJob("job-1");

        assertThat(job.getProviderTaskId()).isEqualTo("task-123");
        assertThat(job.getInternalStatus()).isEqualTo(InternalJobStatus.SUBMITTED);
        assertThat(job.getProviderStatus()).isEqualTo(ProviderJobStatus.PENDING);
        assertThat(job.getNextPollAt()).isNotNull();
        verify(generationJobRepository).save(job);
    }

    @Test
    void submitJob_providerFailureMarksJobFailed() {
        GenerationJob job = baseJob();
        when(generationJobRepository.findById("job-1")).thenReturn(Optional.of(job));
        when(featureFlagsProperties.isProviderSubmissionEnabled()).thenReturn(true);
        when(providerProperties.getCallbackBaseUrl()).thenReturn("http://localhost:8080");
        when(sunoClient.submitGeneration(any()))
                .thenThrow(new ProviderIntegrationException(HttpStatus.BAD_GATEWAY, "PROVIDER_UNAUTHORIZED", "Provider authentication failed"));
        when(generationJobRepository.save(any(GenerationJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> service.submitJob("job-1"))
                .isInstanceOf(ProviderIntegrationException.class)
                .hasMessage("Provider authentication failed");

        assertThat(job.getInternalStatus()).isEqualTo(InternalJobStatus.FAILED);
        assertThat(job.getProviderStatus()).isEqualTo(ProviderJobStatus.CREATE_TASK_FAILED);
    }

    private GenerationJob baseJob() {
        return GenerationJob.builder()
                .id("job-1")
                .projectId("project-1")
                .templateId("template-1")
                .sourceType(JobSourceType.MANUAL)
                .internalStatus(InternalJobStatus.VALIDATED)
                .providerStatus(ProviderJobStatus.NOT_SUBMITTED)
                .promptFinal("prompt")
                .styleFinal("style")
                .titleFinal("title")
                .customMode(true)
                .instrumental(false)
                .model(GenerationModel.V4)
                .build();
    }
}
