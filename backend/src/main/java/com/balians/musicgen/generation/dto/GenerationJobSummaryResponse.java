package com.balians.musicgen.generation.dto;

import com.balians.musicgen.common.enums.GenerationModel;
import com.balians.musicgen.common.enums.InternalJobStatus;
import com.balians.musicgen.common.enums.JobSourceType;
import com.balians.musicgen.common.enums.ProviderJobStatus;
import java.time.Instant;

public record GenerationJobSummaryResponse(
        String id,
        String projectId,
        String templateId,
        String lyricId,
        String lyricTitle,
        JobSourceType sourceType,
        InternalJobStatus internalStatus,
        ProviderJobStatus providerStatus,
        String providerTaskId,
        String titleFinal,
        GenerationModel model,
        Instant createdAt,
        Instant updatedAt
) {
}
