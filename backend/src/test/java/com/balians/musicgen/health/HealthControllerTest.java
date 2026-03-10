package com.balians.musicgen.health;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

    @Mock
    private AppReadinessService appReadinessService;

    @InjectMocks
    private HealthController healthController;

    @Test
    void health_returnsLightweightReadinessPayload() throws Exception {
        when(appReadinessService.readiness()).thenReturn(Map.of(
                "status", "UP",
                "application", "musicgen-backend",
                "timestamp", Instant.now(),
                "mongoReachable", true,
                "providerConfigured", true
        ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(healthController).build();

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.mongoReachable").value(true))
                .andExpect(jsonPath("$.data.providerConfigured").value(true));
    }
}
