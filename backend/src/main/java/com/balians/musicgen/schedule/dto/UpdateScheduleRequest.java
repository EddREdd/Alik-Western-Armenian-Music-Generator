package com.balians.musicgen.schedule.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateScheduleRequest(
        @NotBlank(message = "templateId is required")
        @Size(max = 100, message = "templateId must be at most 100 characters")
        String templateId,
        @NotBlank(message = "name is required")
        @Size(max = 120, message = "name must be at most 120 characters")
        String name,
        @NotBlank(message = "timezone is required")
        @Size(max = 100, message = "timezone must be at most 100 characters")
        String timezone,
        @NotBlank(message = "cronExpression is required")
        @Size(max = 100, message = "cronExpression must be at most 100 characters")
        String cronExpression,
        @NotNull(message = "enabled is required")
        Boolean enabled,
        @NotNull(message = "autoSubmitToProvider is required")
        Boolean autoSubmitToProvider,
        @Min(value = 0, message = "creditsMinThreshold must be at least 0")
        @Max(value = 100000, message = "creditsMinThreshold must be at most 100000")
        Integer creditsMinThreshold
) {
}
