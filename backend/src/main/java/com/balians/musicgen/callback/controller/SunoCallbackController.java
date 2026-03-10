package com.balians.musicgen.callback.controller;

import com.balians.musicgen.callback.dto.SunoCallbackAckResponse;
import com.balians.musicgen.callback.service.SunoCallbackService;
import com.balians.musicgen.common.exception.BadRequestException;
import com.balians.musicgen.common.response.StandardSuccessResponse;
import com.balians.musicgen.config.FeatureFlagsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/integrations/suno")
public class SunoCallbackController {

    private final SunoCallbackService sunoCallbackService;
    private final FeatureFlagsProperties featureFlagsProperties;

    @PostMapping(value = "/callback", consumes = MediaType.APPLICATION_JSON_VALUE)
    public StandardSuccessResponse<SunoCallbackAckResponse> handleCallback(@RequestBody String rawPayload) {
        if (!featureFlagsProperties.isCallbackProcessingEnabled()) {
            throw new BadRequestException("Callback processing is disabled by configuration");
        }
        sunoCallbackService.handleCallback(rawPayload);
        return StandardSuccessResponse.ok(new SunoCallbackAckResponse("received"));
    }
}
