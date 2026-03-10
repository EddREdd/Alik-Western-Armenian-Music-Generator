package com.balians.musicgen.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.balians.musicgen.admin.service.AdminOperationsService;
import com.balians.musicgen.common.exception.GlobalExceptionHandler;
import com.balians.musicgen.common.exception.NotFoundException;
import com.balians.musicgen.config.FeatureFlagsProperties;
import com.balians.musicgen.schedule.service.ScheduleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminOperationsService adminOperationsService;
    @Mock
    private ScheduleService scheduleService;
    @Mock
    private FeatureFlagsProperties featureFlagsProperties;

    @InjectMocks
    private AdminController adminController;

    @Test
    void getJob_notFoundReturnsConsistentErrorContract() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(adminController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        when(adminOperationsService.getGenerationJob("missing")).thenThrow(new NotFoundException("Generation job not found: missing"));
        when(featureFlagsProperties.isAdminEndpointsEnabled()).thenReturn(true);

        mockMvc.perform(get("/api/v1/admin/generation-jobs/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Generation job not found: missing"));
    }
}
