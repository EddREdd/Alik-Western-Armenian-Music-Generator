package com.balians.musicgen.admin.dto;

public record AdminMetricSnapshotResponse(
        long daily,
        long weekly,
        long monthly,
        long total
) {
}
