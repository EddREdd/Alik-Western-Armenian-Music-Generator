package com.balians.musicgen.startup;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.balians.musicgen.config.FeatureFlagsProperties;
import com.balians.musicgen.media.config.MediaStorageProperties;
import com.balians.musicgen.provider.config.ProviderProperties;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.junit.jupiter.api.Test;

class StartupValidationServiceTest {

    @Test
    void validate_acceptsDisabledProviderSubmissionWithoutApiKey() {
        FeatureFlagsProperties flags = new FeatureFlagsProperties();
        flags.setProviderSubmissionEnabled(false);
        ProviderProperties provider = new ProviderProperties();
        provider.setBaseUrl("https://api.sunoapi.org");
        provider.setApiKey("change-me");
        provider.setCallbackBaseUrl("http://localhost:8080");
        MongoProperties mongo = new MongoProperties();
        mongo.setUri("mongodb://example.com:27017/alike_db");

        StartupValidationService service = new StartupValidationService(flags, provider, localMediaStorage(), mongo);

        assertThatCode(service::validate).doesNotThrowAnyException();
    }

    @Test
    void validate_rejectsMissingApiKeyWhenProviderSubmissionEnabled() {
        FeatureFlagsProperties flags = new FeatureFlagsProperties();
        flags.setProviderSubmissionEnabled(true);
        ProviderProperties provider = new ProviderProperties();
        provider.setBaseUrl("https://api.sunoapi.org");
        provider.setApiKey("change-me");
        provider.setCallbackBaseUrl("http://localhost:8080");
        MongoProperties mongo = new MongoProperties();
        mongo.setUri("mongodb://example.com:27017/alike_db");

        StartupValidationService service = new StartupValidationService(flags, provider, localMediaStorage(), mongo);

        assertThatThrownBy(service::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("provider.api-key");
    }

    @Test
    void validate_rejectsLocalMongoUri() {
        FeatureFlagsProperties flags = new FeatureFlagsProperties();
        flags.setProviderSubmissionEnabled(false);
        ProviderProperties provider = new ProviderProperties();
        provider.setBaseUrl("https://api.sunoapi.org");
        provider.setApiKey("change-me");
        provider.setCallbackBaseUrl("http://localhost:8080");
        MongoProperties mongo = new MongoProperties();
        mongo.setUri("mongodb://localhost:27017/alike_db");

        StartupValidationService service = new StartupValidationService(flags, provider, localMediaStorage(), mongo);

        assertThatThrownBy(service::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("server database");
    }

    @Test
    void validate_rejectsIncompleteSpacesConfiguration() {
        FeatureFlagsProperties flags = new FeatureFlagsProperties();
        flags.setProviderSubmissionEnabled(false);
        ProviderProperties provider = new ProviderProperties();
        provider.setBaseUrl("https://api.sunoapi.org");
        provider.setApiKey("change-me");
        provider.setCallbackBaseUrl("http://localhost:8080");
        MediaStorageProperties mediaStorage = new MediaStorageProperties();
        mediaStorage.setType("spaces");
        mediaStorage.setSpacesEndpoint("http://minio:9000");
        MongoProperties mongo = new MongoProperties();
        mongo.setUri("mongodb://example.com:27017/alike_db");

        StartupValidationService service = new StartupValidationService(flags, provider, mediaStorage, mongo);

        assertThatThrownBy(service::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("media.storage.spaces-region");
    }

    private MediaStorageProperties localMediaStorage() {
        MediaStorageProperties mediaStorage = new MediaStorageProperties();
        mediaStorage.setType("local");
        return mediaStorage;
    }
}
