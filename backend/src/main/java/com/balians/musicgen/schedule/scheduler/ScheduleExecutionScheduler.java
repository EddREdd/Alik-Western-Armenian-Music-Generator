package com.balians.musicgen.schedule.scheduler;

import com.balians.musicgen.schedule.service.ScheduleExecutionProperties;
import com.balians.musicgen.schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleExecutionScheduler {

    private final ScheduleService scheduleService;
    private final ScheduleExecutionProperties scheduleExecutionProperties;

    @Scheduled(fixedDelayString = "${schedules.execution.interval-ms:60000}")
    public void executeDueSchedules() {
        if (!scheduleExecutionProperties.isEnabled()) {
            return;
        }
        log.debug("Running schedule execution scan");
        scheduleService.executeDueSchedules();
    }
}
