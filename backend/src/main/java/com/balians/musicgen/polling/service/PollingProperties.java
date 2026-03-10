package com.balians.musicgen.polling.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "polling")
public class PollingProperties {

    private boolean enabled = true;
    private long intervalMs = 60_000;
    private int batchSize = 10;
    private long baseDelaySeconds = 60;
    private long maxDelaySeconds = 900;
}
