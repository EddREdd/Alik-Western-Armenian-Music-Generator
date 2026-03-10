package com.balians.musicgen.provider.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.JsonNode;

public record SunoRecordInfoDataDto(
        String status,
        Integer errorCode,
        String errorMessage,
        SunoRecordInfoResultDto response,
        @JsonAlias({"taskId", "task_id"}) String taskId,
        JsonNode param,
        String type
) {
}
