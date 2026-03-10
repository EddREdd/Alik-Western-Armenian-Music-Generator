package com.balians.musicgen.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "features")
public class FeatureFlagsProperties {

    private boolean providerSubmissionEnabled = true;
    private boolean callbackProcessingEnabled = true;
    private boolean adminEndpointsEnabled = true;
}
