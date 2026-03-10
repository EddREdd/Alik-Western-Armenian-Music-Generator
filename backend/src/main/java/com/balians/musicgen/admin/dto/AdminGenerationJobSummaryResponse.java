package com.balians.musicgen.admin.dto;

import com.balians.musicgen.common.enums.InternalJobStatus;
import com.balians.musicgen.common.enums.JobSourceType;
import com.balians.musicgen.common.enums.ProviderJobStatus;
import java.time.Instant;

public record AdminGenerationJobSummaryResponse(
        String id,
        String projectId,
        String templateId,
        JobSourceType sourceType,
        InternalJobStatus internalStatus,
        ProviderJobStatus providerStatus,
        String providerTaskId,
        String errorCode,
        String errorMessage,
        Instant createdAt,
        Instant submittedAt,
        Instant completedAt,
        Instant failedAt,
        Instant nextPollAt,
        Integer pollAttemptCount,
        boolean stuck
) {
}
