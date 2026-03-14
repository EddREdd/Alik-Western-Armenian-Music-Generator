package com.balians.musicgen.auth.dto;

public record AuthUserResponse(
        String id,
        String email,
        boolean emailVerified,
        boolean hasPassword,
        boolean googleLinked,
        boolean admin,
        boolean unlimitedCredits,
        Integer creditsRemaining,
        Integer creditsUsed,
        Integer songsGenerated,
        boolean frozen,
        String freezeReason
) {
}
