package com.balians.musicgen.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleTokenInfoResponse(
        String iss,
        String aud,
        String sub,
        String email,
        @JsonProperty("email_verified")
        String emailVerified,
        String exp
) {
}
