package com.balians.musicgen.polling.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.balians.musicgen.callback.service.GenerationTrackUpsertService;
import com.balians.musicgen.common.enums.InternalJobStatus;
import com.balians.musicgen.common.enums.ProviderJobStatus;
import com.balians.musicgen.generation.mapper.GenerationJobMapper;
import com.balians.musicgen.generation.model.GenerationJob;
import com.balians.musicgen.generation.repository.GenerationJobRepository;
import com.balians.musicgen.generation.repository.GenerationTrackRepository;
import com.balians.musicgen.polling.repository.PollAttemptRepository;
import com.balians.musicgen.provider.client.SunoClient;
import com.balians.musicgen.provider.dto.SunoRecordInfoDataDto;
import com.balians.musicgen.provider.dto.SunoRecordInfoResponse;
import com.balians.musicgen.provider.dto.SunoRecordInfoResultDto;
import com.balians.musicgen.provider.dto.SunoRecordInfoTrackDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PollingReconciliationServiceTest {

    @Mock private GenerationJobRepository generationJobRepository;
    @Mock private GenerationTrackRepository generationTrackRepository;
    @Mock private PollAttemptRepository pollAttemptRepository;
    @Mock private GenerationJobMapper generationJobMapper;
    @Mock private GenerationTrackUpsertService generationTrackUpsertService;
    @Mock private SunoClient sunoClient;

    @InjectMocks
    private PollingReconciliationService service;

    @Test
    void reconcileNow_successCompletesJobAndRepairsTracks() {
        PollingProperties properties = new PollingProperties();
        properties.setBatchSize(10);
        properties.setBaseDelaySeconds(60);
        properties.setMaxDelaySeconds(900);
        service = new PollingReconciliationService(
                generationJobRepository, generationTrackRepository, pollAttemptRepository, generationJobMapper,
                generationTrackUpsertService, properties, sunoClient, new ObjectMapper()
        );

        GenerationJob job = GenerationJob.builder()
                .id("job-1")
                .providerTaskId("task-1")
                .internalStatus(InternalJobStatus.SUBMITTED)
                .providerStatus(ProviderJobStatus.PENDING)
                .build();

        when(generationJobRepository.findById("job-1")).thenReturn(Optional.of(job), Optional.of(job));
        when(sunoClient.getRecordInfo("task-1")).thenReturn(new SunoRecordInfoResponse(
                200,
                "ok",
                new SunoRecordInfoDataDto("SUCCESS", 0, null,
                        new SunoRecordInfoResultDto(List.of(new SunoRecordInfoTrackDto("track-1", "audio", "stream", "image", "prompt", "V4", "title", "tag", null, 100))),
                        "task-1", null, "music")
        ));
        when(generationTrackUpsertService.upsertProviderTracks(any(), any())).thenReturn(1);
        when(generationJobRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.reconcileNow("job-1");

        assertThat(job.getInternalStatus()).isEqualTo(InternalJobStatus.COMPLETED);
        assertThat(job.getProviderStatus()).isEqualTo(ProviderJobStatus.SUCCESS);
        assertThat(job.getNextPollAt()).isNull();
    }
}
