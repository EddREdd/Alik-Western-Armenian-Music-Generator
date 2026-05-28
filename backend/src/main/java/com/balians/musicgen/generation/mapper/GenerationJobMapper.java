package com.balians.musicgen.generation.mapper;

import com.balians.musicgen.generation.dto.GenerationJobResponse;
import com.balians.musicgen.generation.dto.GenerationJobSummaryResponse;
import com.balians.musicgen.generation.dto.GenerationStatusHistoryResponse;
import com.balians.musicgen.generation.dto.GenerationTrackResponse;
import com.balians.musicgen.generation.model.GenerationJob;
import com.balians.musicgen.generation.model.GenerationTrack;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GenerationJobMapper {

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    public GenerationJobSummaryResponse toSummary(GenerationJob job) {
        return new GenerationJobSummaryResponse(
                job.getId(),
                job.getProjectId(),
                job.getTemplateId(),
                job.getLyricId(),
                job.getLyricTitle(),
                job.getSourceType(),
                job.getInternalStatus(),
                job.getProviderStatus(),
                job.getProviderTaskId(),
                job.getTitleFinal(),
                job.getModel(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }

    public GenerationJobResponse toResponse(GenerationJob job, List<GenerationTrack> tracks) {
        List<GenerationStatusHistoryResponse> historyResponses = job.getStatusHistory()
                .stream()
                .map(entry -> new GenerationStatusHistoryResponse(
                        entry.getInternalStatus(),
                        entry.getProviderStatus(),
                        entry.getMessage(),
                        entry.getChangedAt()
                ))
                .toList();

        List<GenerationTrackResponse> trackResponses = tracks.stream()
                .map(track -> {
                    String playableAudio = firstNonBlank(
                            track.getLocalAudioUrl(),
                            track.getAudioUrl(),
                            track.getStreamAudioUrl()
                    );
                    String playableStream = firstNonBlank(
                            track.getLocalAudioUrl(),
                            track.getStreamAudioUrl(),
                            track.getAudioUrl()
                    );
                    String playableImage = firstNonBlank(
                            track.getLocalImageUrl(),
                            track.getImageUrl()
                    );
                    return new GenerationTrackResponse(
                            track.getId(),
                            track.getProviderMusicId(),
                            track.getTrackIndex(),
                            playableAudio,
                            playableStream,
                            playableImage,
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
                })
                .toList();

        return new GenerationJobResponse(
                job.getId(),
                job.getProjectId(),
                job.getTemplateId(),
                job.getLyricId(),
                job.getLyricTitle(),
                job.getSourceType(),
                job.getInternalStatus(),
                job.getProviderStatus(),
                job.getProviderTaskId(),
                job.getPromptFinal(),
                job.getStyleFinal(),
                job.getTitleFinal(),
                job.getCustomMode(),
                job.getInstrumental(),
                job.getModel(),
                job.getErrorCode(),
                job.getErrorMessage(),
                job.getCreatedAt(),
                job.getUpdatedAt(),
                job.getSubmittedAt(),
                job.getCompletedAt(),
                job.getFailedAt(),
                historyResponses,
                trackResponses
        );
    }
}
