package com.balians.musicgen.admin.dto;

import java.time.Instant;

public record AdminUserDetailResponse(
        String id,
        String email,
        Instant joinDate,
        Integer generationCount,
        Integer creditsRemaining,
        Integer creditsUsed,
        Integer songsGenerated,
        boolean emailVerified,
        String inviteCode,
        boolean frozen,
        Instant frozenAt,
        String freezeReason,
        boolean admin,
        boolean unlimitedCredits,
        String googleEmail
) {
}
