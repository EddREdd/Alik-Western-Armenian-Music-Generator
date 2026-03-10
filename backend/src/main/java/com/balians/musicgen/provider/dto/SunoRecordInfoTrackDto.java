package com.balians.musicgen.provider.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record SunoRecordInfoTrackDto(
        String id,
        @JsonAlias({"audioUrl", "audio_url"}) String audioUrl,
        @JsonAlias({"streamAudioUrl", "stream_audio_url"}) String streamAudioUrl,
        @JsonAlias({"imageUrl", "image_url"}) String imageUrl,
        String prompt,
        @JsonAlias({"modelName", "model_name"}) String modelName,
        String title,
        String tags,
        String createTime,
        Integer duration
) {
}
