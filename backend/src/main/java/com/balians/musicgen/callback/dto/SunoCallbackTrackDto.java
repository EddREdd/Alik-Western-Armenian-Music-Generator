package com.balians.musicgen.callback.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record SunoCallbackTrackDto(
        String id,
        @JsonAlias("audio_url") String audioUrl,
        @JsonAlias("stream_audio_url") String streamAudioUrl,
        @JsonAlias("image_url") String imageUrl,
        String prompt,
        @JsonAlias("model_name") String modelName,
        String title,
        String tags,
        String createTime,
        Integer duration
) {
}
