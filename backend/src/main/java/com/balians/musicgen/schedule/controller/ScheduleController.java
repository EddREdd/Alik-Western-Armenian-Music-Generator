package com.balians.musicgen.schedule.controller;

import com.balians.musicgen.common.response.StandardSuccessResponse;
import com.balians.musicgen.schedule.dto.CreateScheduleRequest;
import com.balians.musicgen.schedule.dto.ScheduleDefinitionResponse;
import com.balians.musicgen.schedule.dto.ScheduleRunResponse;
import com.balians.musicgen.schedule.dto.UpdateScheduleRequest;
import com.balians.musicgen.schedule.service.ScheduleService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    @PostMapping
    public StandardSuccessResponse<ScheduleDefinitionResponse> createSchedule(
            @Valid @RequestBody CreateScheduleRequest request
    ) {
        return StandardSuccessResponse.ok(scheduleService.createSchedule(request));
    }

    @PutMapping("/{id}")
    public StandardSuccessResponse<ScheduleDefinitionResponse> updateSchedule(
            @PathVariable String id,
            @Valid @RequestBody UpdateScheduleRequest request
    ) {
        return StandardSuccessResponse.ok(scheduleService.updateSchedule(id, request));
    }

    @GetMapping
    public StandardSuccessResponse<List<ScheduleDefinitionResponse>> listSchedules() {
        return StandardSuccessResponse.ok(scheduleService.listSchedules());
    }

    @GetMapping("/due")
    public StandardSuccessResponse<List<ScheduleDefinitionResponse>> listDueSchedules() {
        return StandardSuccessResponse.ok(scheduleService.listDueSchedules());
    }

    @GetMapping("/{id}")
    public StandardSuccessResponse<ScheduleDefinitionResponse> getSchedule(@PathVariable String id) {
        return StandardSuccessResponse.ok(scheduleService.getSchedule(id));
    }

    @PostMapping("/{id}/enable")
    public StandardSuccessResponse<ScheduleDefinitionResponse> enableSchedule(@PathVariable String id) {
        return StandardSuccessResponse.ok(scheduleService.enableSchedule(id));
    }

    @PostMapping("/{id}/disable")
    public StandardSuccessResponse<ScheduleDefinitionResponse> disableSchedule(@PathVariable String id) {
        return StandardSuccessResponse.ok(scheduleService.disableSchedule(id));
    }

    @PostMapping("/{id}/run-now")
    public StandardSuccessResponse<ScheduleRunResponse> runNow(@PathVariable String id) {
        return StandardSuccessResponse.ok(scheduleService.runNow(id));
    }

    @GetMapping("/{id}/runs")
    public StandardSuccessResponse<List<ScheduleRunResponse>> getRuns(@PathVariable String id) {
        return StandardSuccessResponse.ok(scheduleService.getScheduleRuns(id));
    }
}
