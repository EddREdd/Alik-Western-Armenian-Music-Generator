package com.balians.musicgen.admin.dto;

import com.balians.musicgen.common.enums.InternalJobStatus;
import com.balians.musicgen.common.enums.JobSourceType;
import com.balians.musicgen.common.enums.ProviderJobStatus;
import java.time.Instant;

public record AdminGenerationJobFilter(
        String projectId,
        InternalJobStatus internalStatus,
        ProviderJobStatus providerStatus,
        JobSourceType sourceType,
        String providerTaskId,
        Instant createdFrom,
        Instant createdTo,
        Boolean failedOnly,
        Boolean stuckOnly,
        Integer page,
        Integer size
) {
}
