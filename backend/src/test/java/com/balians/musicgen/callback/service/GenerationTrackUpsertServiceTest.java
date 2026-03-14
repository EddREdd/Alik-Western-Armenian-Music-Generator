package com.balians.musicgen.callback.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.balians.musicgen.callback.dto.SunoCallbackTrackDto;
import com.balians.musicgen.generation.model.GenerationJob;
import com.balians.musicgen.generation.model.GenerationTrack;
import com.balians.musicgen.generation.repository.GenerationTrackRepository;
import com.balians.musicgen.media.service.TrackMediaStorageService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GenerationTrackUpsertServiceTest {

    @Mock
    private GenerationTrackRepository generationTrackRepository;
    @Mock
    private TrackMediaStorageService trackMediaStorageService;

    @InjectMocks
    private GenerationTrackUpsertService service;

    @Test
    void upsertTracks_updatesExistingTrackInsteadOfDuplicating() {
        GenerationJob job = GenerationJob.builder().id("job-1").build();
        GenerationTrack existing = GenerationTrack.builder().id("track-local").generationJobId("job-1").providerMusicId("provider-1").build();
        when(generationTrackRepository.findByGenerationJobIdAndProviderMusicId("job-1", "provider-1"))
                .thenReturn(Optional.of(existing));
        when(generationTrackRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        int upserted = service.upsertTracks(job, List.of(new SunoCallbackTrackDto(
                "provider-1", "audio.mp3", "stream.mp3", "image.jpg", "prompt", "V4", "title", "tag1,tag2", null, 123
        )));

        assertThat(upserted).isEqualTo(1);
        assertThat(existing.getAudioUrl()).isEqualTo("audio.mp3");
        assertThat(existing.getTags()).containsExactly("tag1", "tag2");
    }
}
