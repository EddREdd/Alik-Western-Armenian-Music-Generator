package com.balians.musicgen.generation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.balians.musicgen.auth.model.UserAccount;
import com.balians.musicgen.common.enums.GenerationModel;
import com.balians.musicgen.common.enums.JobSourceType;
import com.balians.musicgen.common.exception.BadRequestException;
import com.balians.musicgen.common.exception.NotFoundException;
import com.balians.musicgen.lyrics.dto.LyricResponse;
import com.balians.musicgen.generation.dto.CreateGenerationJobRequest;
import com.balians.musicgen.generation.mapper.GenerationJobMapper;
import com.balians.musicgen.generation.model.GenerationJob;
import com.balians.musicgen.generation.repository.GenerationJobRepository;
import com.balians.musicgen.generation.repository.GenerationTrackRepository;
import com.balians.musicgen.lyrics.service.LyricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class GenerationJobServiceTest {

    @Mock
    private GenerationJobRepository generationJobRepository;
    @Mock
    private GenerationTrackRepository generationTrackRepository;
    @Mock
    private GenerationJobMapper generationJobMapper;
    @Mock
    private LyricsService lyricsService;

    @InjectMocks
    private GenerationJobService service;

    @BeforeEach
    void stubLyricsService() {
        lenient().when(lyricsService.resolveProjectId(any(), any())).thenAnswer(invocation -> {
            String projectId = invocation.getArgument(0);
            UserAccount owner = invocation.getArgument(1);
            if (projectId != null && !projectId.toString().isBlank()) {
                return projectId.toString().trim();
            }
            if (owner != null && owner.getId() != null && !owner.getId().isBlank()) {
                return "user-" + owner.getId();
            }
            return "system";
        });
    }

    @Test
    void createJob_missingModelAndInstrumental_defaultsToV5_5AndNonInstrumental() {
        when(generationJobRepository.save(any(GenerationJob.class))).thenAnswer(invocation -> {
            GenerationJob job = invocation.getArgument(0);
            job.setId("job-1");
            return job;
        });

        service.createJob(validRequest(null, null, null), UserAccount.builder().id("user-1").build());

        ArgumentCaptor<GenerationJob> captor = ArgumentCaptor.forClass(GenerationJob.class);
        verify(generationJobRepository).save(captor.capture());
        GenerationJob saved = captor.getValue();
        assertThat(saved.getModel()).isEqualTo(GenerationModel.V5_5);
        assertThat(saved.getInstrumental()).isFalse();
        assertThat(saved.getProjectId()).isEqualTo("user-user-1");
    }

    @Test
    void createJob_withAnotherUsersLyricId_isRejected() {
        when(lyricsService.assertAvailableForGeneration(eq("lyric-owned-by-a"), any(UserAccount.class)))
                .thenThrow(new NotFoundException("Lyric not found: lyric-owned-by-a"));

        assertThatThrownBy(() -> service.createJob(
                new CreateGenerationJobRequest(
                        null,
                        null,
                        "lyric-owned-by-a",
                        JobSourceType.MANUAL,
                        "a".repeat(50),
                        "folk-pop",
                        "Title",
                        true,
                        false,
                        null
                ),
                UserAccount.builder().id("user-b").build()
        ))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Lyric not found: lyric-owned-by-a");
    }

    @Test
    void createJob_withOwnLyricId_linksLyric() {
        when(lyricsService.assertAvailableForGeneration(eq("lyric-1"), any(UserAccount.class)))
                .thenReturn(new LyricResponse(
                        "lyric-1",
                        "user-1",
                        "user-user-1",
                        "Երգ",
                        "a".repeat(50),
                        1,
                        false,
                        10,
                        List.of(),
                        List.of(),
                        "WESTERN_ARMENIAN",
                        false,
                        null,
                        null
                ));
        when(generationJobRepository.save(any(GenerationJob.class))).thenAnswer(invocation -> {
            GenerationJob job = invocation.getArgument(0);
            job.setId("job-1");
            return job;
        });

        service.createJob(
                new CreateGenerationJobRequest(
                        null,
                        null,
                        "lyric-1",
                        JobSourceType.MANUAL,
                        "a".repeat(50),
                        "folk-pop",
                        "Title",
                        true,
                        false,
                        null
                ),
                owner()
        );

        verify(lyricsService).linkToSong("lyric-1", "job-1");
    }

    @Test
    void createJob_instrumentalTrue_isRejected() {
        assertThatThrownBy(() -> service.createJob(validRequest(null, true, null), owner()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Instrumental generation is disabled.");
    }

    @Test
    void createJob_modelOtherThanV5_5_isRejected() {
        assertThatThrownBy(() -> service.createJob(validRequest(GenerationModel.V4, false, null), owner()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Only Suno v5.5 is supported.");
    }

    @Test
    void createJob_promptUnder50Characters_isRejected() {
        assertThatThrownBy(() -> service.createJob(
                new CreateGenerationJobRequest(
                        "project-1",
                        null,
                        null,
                        JobSourceType.MANUAL,
                        "short",
                        "folk-pop",
                        "Title",
                        true,
                        false,
                        null
                ),
                owner()
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Lyrics must be at least 50 characters.");
    }

    @Test
    void createJob_promptWith50Characters_isAccepted() {
        when(generationJobRepository.save(any(GenerationJob.class))).thenAnswer(invocation -> {
            GenerationJob job = invocation.getArgument(0);
            job.setId("job-1");
            return job;
        });

        String lyrics = "a".repeat(50);
        service.createJob(
                new CreateGenerationJobRequest(
                        "project-1",
                        null,
                        null,
                        JobSourceType.MANUAL,
                        lyrics,
                        "folk-pop",
                        "Title",
                        true,
                        false,
                        null
                ),
                owner()
        );

        ArgumentCaptor<GenerationJob> captor = ArgumentCaptor.forClass(GenerationJob.class);
        verify(generationJobRepository).save(captor.capture());
        assertThat(captor.getValue().getPromptFinal()).hasSize(50);
        assertThat(captor.getValue().getModel()).isEqualTo(GenerationModel.V5_5);
    }

    private static UserAccount owner() {
        return UserAccount.builder().id("user-1").build();
    }

    private static CreateGenerationJobRequest validRequest(
            GenerationModel model,
            Boolean instrumental,
            String projectId
    ) {
        return new CreateGenerationJobRequest(
                projectId,
                null,
                null,
                JobSourceType.MANUAL,
                "a".repeat(50),
                "folk-pop",
                "Title",
                true,
                instrumental,
                model
        );
    }
}
