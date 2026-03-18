package com.balians.musicgen.startup;

import com.balians.musicgen.config.FeatureFlagsProperties;
import com.balians.musicgen.provider.config.ProviderProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StartupValidationService {

    private final FeatureFlagsProperties featureFlagsProperties;
    private final ProviderProperties providerProperties;
    private final MongoProperties mongoProperties;

    @PostConstruct
    public void validate() {
        requireText(mongoProperties.getUri(), "spring.data.mongodb.uri is required");
        rejectLocalMongoUri(mongoProperties.getUri());

        if (featureFlagsProperties.isProviderSubmissionEnabled()) {
            requireText(providerProperties.getBaseUrl(), "provider.base-url is required when provider submission is enabled");
            requireText(providerProperties.getApiKey(), "provider.api-key is required when provider submission is enabled");
            requireText(providerProperties.getCallbackBaseUrl(), "provider.callback-base-url is required when provider submission is enabled");
        }

        log.info("Startup configuration validated: providerSubmissionEnabled={}, callbackProcessingEnabled={}, adminEndpointsEnabled={}",
                featureFlagsProperties.isProviderSubmissionEnabled(),
                featureFlagsProperties.isCallbackProcessingEnabled(),
                featureFlagsProperties.isAdminEndpointsEnabled());
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank() || "change-me".equalsIgnoreCase(value.trim())) {
            throw new IllegalStateException(message);
        }
    }

    private void rejectLocalMongoUri(String uri) {
        String normalized = uri.trim().toLowerCase();
        if (normalized.contains("localhost") || normalized.contains("127.0.0.1")) {
            throw new IllegalStateException("spring.data.mongodb.uri must point to the server database, not localhost");
        }
    }
}
