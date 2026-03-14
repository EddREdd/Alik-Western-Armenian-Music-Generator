package com.balians.musicgen.admin.dto;

public record AdminDashboardResponse(
        AdminMetricSnapshotResponse songVolume,
        AdminMetricSnapshotResponse registrationTrends,
        AdminMetricSnapshotResponse creditConsumption,
        long totalUsers,
        long frozenUsers,
        long activeInviteCodes,
        long usedInviteCodes,
        long totalLyrics,
        long totalSongs
) {
}
