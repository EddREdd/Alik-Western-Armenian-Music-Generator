package com.balians.musicgen.polling.scheduler;

import com.balians.musicgen.polling.service.PollingProperties;
import com.balians.musicgen.polling.service.PollingReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PollingScheduler {

    private final PollingReconciliationService pollingReconciliationService;
    private final PollingProperties pollingProperties;

    @Scheduled(fixedDelayString = "${polling.interval-ms:60000}")
    public void pollActiveJobs() {
        if (!pollingProperties.isEnabled()) {
            return;
        }
        log.debug("Running polling scheduler");
        pollingReconciliationService.pollDueJobs();
    }
}
