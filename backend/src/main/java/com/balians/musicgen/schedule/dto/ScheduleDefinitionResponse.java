package com.balians.musicgen.schedule.dto;

import java.time.Instant;

public record ScheduleDefinitionResponse(
        String id,
        String projectId,
        String templateId,
        String name,
        String timezone,
        String cronExpression,
        Boolean enabled,
        Boolean autoSubmitToProvider,
        Integer creditsMinThreshold,
        Instant lastRunAt,
        Instant nextRunAt,
        Instant createdAt,
        Instant updatedAt
) {
}
