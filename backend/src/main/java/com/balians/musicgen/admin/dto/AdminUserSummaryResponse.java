package com.balians.musicgen.admin.dto;

import java.time.Instant;

public record AdminUserSummaryResponse(
        String id,
        String email,
        Instant joinDate,
        Integer generationCount,
        Integer creditsRemaining,
        String inviteCode,
        boolean frozen,
        boolean admin,
        boolean unlimitedCredits
) {
}
