package com.balians.musicgen.callback.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;

public record SunoCallbackDataDto(
        String callbackType,
        @JsonAlias({"task_id", "taskId"}) String taskId,
        @JsonAlias("data") List<SunoCallbackTrackDto> tracks
) {
}
