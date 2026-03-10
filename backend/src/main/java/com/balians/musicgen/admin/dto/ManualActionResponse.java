package com.balians.musicgen.admin.dto;

public record ManualActionResponse(
        String action,
        String targetId,
        String status,
        String message
) {
}
