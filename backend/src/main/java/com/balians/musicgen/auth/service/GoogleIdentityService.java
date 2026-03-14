package com.balians.musicgen.auth.service;

import com.balians.musicgen.auth.dto.GoogleTokenInfoResponse;
import com.balians.musicgen.common.exception.BadRequestException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@RequiredArgsConstructor
public class GoogleIdentityService {

    private final AuthProperties authProperties;
    private final RestClient restClient = RestClient.builder().build();

    public GoogleIdentity verifyIdToken(String idToken) {
        GoogleTokenInfoResponse response;
        try {
            response = restClient.get()
                    .uri(authProperties.getGoogleTokenInfoUrl() + "?id_token={idToken}", idToken)
                    .retrieve()
                    .body(GoogleTokenInfoResponse.class);
        } catch (RestClientException ex) {
            throw new BadRequestException("Google token verification failed");
        }

        if (response == null || response.sub() == null || response.sub().isBlank()) {
            throw new BadRequestException("Google token verification failed");
        }
        if (!"true".equalsIgnoreCase(response.emailVerified())) {
            throw new BadRequestException("Google account email is not verified");
        }

        String configuredClientId = authProperties.getGoogleClientId();
        if (configuredClientId != null
                && !configuredClientId.isBlank()
                && !configuredClientId.equals(response.aud())) {
            throw new BadRequestException("Google token audience is invalid");
        }

        if (response.exp() != null && !response.exp().isBlank()) {
            long expiresAt = Long.parseLong(response.exp());
            if (Instant.ofEpochSecond(expiresAt).isBefore(Instant.now())) {
                throw new BadRequestException("Google token has expired");
            }
        }

        if (!"https://accounts.google.com".equals(response.iss())
                && !"accounts.google.com".equals(response.iss())) {
            throw new BadRequestException("Google token issuer is invalid");
        }

        return new GoogleIdentity(
                response.sub(),
                response.email() == null ? null : response.email().trim().toLowerCase()
        );
    }

    public record GoogleIdentity(
            String subject,
            String email
    ) {
    }
}
