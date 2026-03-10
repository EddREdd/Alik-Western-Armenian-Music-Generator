package com.balians.musicgen.admin.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "ops")
public class OpsProperties {

    private long stuckThresholdMinutes = 30;
    private int rawPayloadPreviewLength = 200;
}
