package com.balians.musicgen.admin.dto;

import com.balians.musicgen.common.enums.CallbackProcessingStatus;
import java.time.Instant;

public record CallbackEventSummaryResponse(
        String id,
        String providerTaskId,
        String callbackType,
        String callbackCode,
        String message,
        CallbackProcessingStatus processingStatus,
        Instant receivedAt,
        Instant processedAt,
        String processingError,
        String payloadPreview
) {
}
