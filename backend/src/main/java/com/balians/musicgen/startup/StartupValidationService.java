package com.balians.musicgen.startup;

import com.balians.musicgen.config.FeatureFlagsProperties;
import com.balians.musicgen.provider.config.ProviderProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StartupValidationService {

    private final FeatureFlagsProperties featureFlagsProperties;
    private final ProviderProperties providerProperties;

    @PostConstruct
    public void validate() {
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
}
