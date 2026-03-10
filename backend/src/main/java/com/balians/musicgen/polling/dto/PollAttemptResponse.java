package com.balians.musicgen.polling.dto;

import com.balians.musicgen.polling.model.PollAttemptOutcome;
import java.time.Instant;

public record PollAttemptResponse(
        String id,
        String generationJobId,
        String providerTaskId,
        Integer attemptNo,
        Instant requestedAt,
        String providerStatus,
        Integer responseCode,
        Instant nextPollAt,
        PollAttemptOutcome outcome,
        String errorMessage
) {
}
