package com.balians.musicgen.health;

import com.balians.musicgen.common.response.StandardSuccessResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/health")
public class HealthController {

    private final AppReadinessService appReadinessService;

    @GetMapping
    public StandardSuccessResponse<Map<String, Object>> health() {
        return StandardSuccessResponse.ok(appReadinessService.readiness());
    }
}
