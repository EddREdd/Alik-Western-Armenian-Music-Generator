package com.balians.musicgen.generation.dto;

import com.balians.musicgen.common.enums.GenerationModel;
import com.balians.musicgen.common.enums.InternalJobStatus;
import com.balians.musicgen.common.enums.JobSourceType;
import com.balians.musicgen.common.enums.ProviderJobStatus;
import java.time.Instant;
import java.util.List;

public record GenerationJobResponse(
        String id,
        String projectId,
        String templateId,
        String lyricId,
        String lyricTitle,
        JobSourceType sourceType,
        InternalJobStatus internalStatus,
        ProviderJobStatus providerStatus,
        String providerTaskId,
        String promptFinal,
        String styleFinal,
        String titleFinal,
        Boolean customMode,
        Boolean instrumental,
        GenerationModel model,
        String errorCode,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt,
        Instant submittedAt,
        Instant completedAt,
        Instant failedAt,
        List<GenerationStatusHistoryResponse> statusHistory,
        List<GenerationTrackResponse> tracks
) {
}
