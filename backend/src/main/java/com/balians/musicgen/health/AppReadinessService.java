package com.balians.musicgen.health;

import com.balians.musicgen.config.AppProperties;
import com.balians.musicgen.config.FeatureFlagsProperties;
import com.balians.musicgen.provider.config.ProviderProperties;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppReadinessService {

    private final AppProperties appProperties;
    private final FeatureFlagsProperties featureFlagsProperties;
    private final ProviderProperties providerProperties;
    private final MongoTemplate mongoTemplate;

    public Map<String, Object> readiness() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", mongoReachable() ? "UP" : "DEGRADED");
        response.put("application", appProperties.getName());
        response.put("timestamp", Instant.now());
        response.put("mongoReachable", mongoReachable());
        response.put("providerConfigured", providerConfigured());
        response.put("features", Map.of(
                "providerSubmissionEnabled", featureFlagsProperties.isProviderSubmissionEnabled(),
                "callbackProcessingEnabled", featureFlagsProperties.isCallbackProcessingEnabled(),
                "adminEndpointsEnabled", featureFlagsProperties.isAdminEndpointsEnabled()
        ));
        return response;
    }

    private boolean providerConfigured() {
        return hasText(providerProperties.getBaseUrl())
                && hasText(providerProperties.getApiKey())
                && !"change-me".equalsIgnoreCase(providerProperties.getApiKey())
                && hasText(providerProperties.getCallbackBaseUrl());
    }

    private boolean mongoReachable() {
        try {
            mongoTemplate.getDb().runCommand(new org.bson.Document("ping", 1));
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
