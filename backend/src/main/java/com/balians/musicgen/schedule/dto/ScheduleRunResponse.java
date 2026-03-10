package com.balians.musicgen.schedule.dto;

import com.balians.musicgen.common.enums.ScheduleRunStatus;
import java.time.Instant;
import java.time.LocalDate;

public record ScheduleRunResponse(
        String id,
        String scheduleDefinitionId,
        String generationJobId,
        LocalDate runDate,
        ScheduleRunStatus status,
        Instant startedAt,
        Instant finishedAt,
        String errorMessage,
        Integer creditsSnapshot,
        Boolean providerSubmittedFlag
) {
}
