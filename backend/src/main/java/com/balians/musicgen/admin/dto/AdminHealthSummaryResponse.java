package com.balians.musicgen.admin.dto;

import java.util.Map;

public record AdminHealthSummaryResponse(
        Map<String, Long> jobsByInternalStatus,
        Map<String, Long> jobsByProviderStatus,
        long failedJobsCount,
        long stuckJobsCount,
        long enabledSchedulesCount,
        long dueSchedulesCount,
        long failedScheduleRunsToday,
        long callbacksReceivedToday,
        long pollAttemptsToday
) {
}
