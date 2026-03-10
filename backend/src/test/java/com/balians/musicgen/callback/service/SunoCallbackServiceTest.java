package com.balians.musicgen.callback.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.balians.musicgen.callback.model.CallbackEvent;
import com.balians.musicgen.callback.repository.CallbackEventRepository;
import com.balians.musicgen.common.enums.CallbackProcessingStatus;
import com.balians.musicgen.common.enums.GenerationModel;
import com.balians.musicgen.common.enums.InternalJobStatus;
import com.balians.musicgen.common.enums.JobSourceType;
import com.balians.musicgen.common.enums.ProviderJobStatus;
import com.balians.musicgen.generation.model.GenerationJob;
import com.balians.musicgen.generation.repository.GenerationJobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SunoCallbackServiceTest {

    @Mock
    private CallbackEventRepository callbackEventRepository;
    @Mock
    private GenerationJobRepository generationJobRepository;
    @Mock
    private GenerationTrackUpsertService generationTrackUpsertService;

    @InjectMocks
    private SunoCallbackService service = new SunoCallbackService(null, null, null, new ObjectMapper());

    @Test
    void handleCallback_completeSuccessUpdatesJobAndStoresProcessedEvent() {
        service = new SunoCallbackService(callbackEventRepository, generationJobRepository, generationTrackUpsertService, new ObjectMapper());
        GenerationJob job = GenerationJob.builder()
                .id("job-1")
                .projectId("project-1")
                .sourceType(JobSourceType.MANUAL)
                .internalStatus(InternalJobStatus.SUBMITTED)
                .providerStatus(ProviderJobStatus.PENDING)
                .providerTaskId("task-1")
                .model(GenerationModel.V4)
                .build();

        when(callbackEventRepository.save(any(CallbackEvent.class))).thenAnswer(invocation -> {
            CallbackEvent event = invocation.getArgument(0);
            if (event.getId() == null) {
                event.setId("event-1");
            }
            return event;
        });
        when(callbackEventRepository.findFirstByProviderTaskIdAndCallbackTypeAndPayloadHashAndProcessingStatusInOrderByReceivedAtDesc(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(generationJobRepository.findByProviderTaskId("task-1")).thenReturn(Optional.of(job));
        when(generationTrackUpsertService.upsertTracks(any(), any())).thenReturn(2);

        service.handleCallback("{\"code\":200,\"msg\":\"ok\",\"data\":{\"callbackType\":\"complete\",\"task_id\":\"task-1\",\"data\":[{\"id\":\"track-1\",\"audio_url\":\"a.mp3\"}]}}");

        assertThat(job.getInternalStatus()).isEqualTo(InternalJobStatus.COMPLETED);
        assertThat(job.getProviderStatus()).isEqualTo(ProviderJobStatus.SUCCESS);
        verify(generationTrackUpsertService).upsertTracks(any(), any());
    }

    @Test
    void handleCallback_duplicateDoesNotReprocessJob() {
        service = new SunoCallbackService(callbackEventRepository, generationJobRepository, generationTrackUpsertService, new ObjectMapper());
        when(callbackEventRepository.save(any(CallbackEvent.class))).thenAnswer(invocation -> {
            CallbackEvent event = invocation.getArgument(0);
            if (event.getId() == null) {
                event.setId("event-1");
            }
            return event;
        });
        when(callbackEventRepository.findFirstByProviderTaskIdAndCallbackTypeAndPayloadHashAndProcessingStatusInOrderByReceivedAtDesc(any(), any(), any(), any()))
                .thenReturn(Optional.of(CallbackEvent.builder().id("existing").processingStatus(CallbackProcessingStatus.PROCESSED).build()));

        service.handleCallback("{\"code\":200,\"msg\":\"ok\",\"data\":{\"callbackType\":\"complete\",\"task_id\":\"task-1\",\"data\":[{\"id\":\"track-1\"}]}}");

        verify(generationJobRepository, never()).findByProviderTaskId(any());
        verify(generationTrackUpsertService, never()).upsertTracks(any(), any());
    }
}
