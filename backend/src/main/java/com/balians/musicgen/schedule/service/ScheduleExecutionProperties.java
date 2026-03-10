package com.balians.musicgen.schedule.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "schedules.execution")
public class ScheduleExecutionProperties {

    private boolean enabled = true;
    private long intervalMs = 60_000;
    private int batchSize = 10;
}
