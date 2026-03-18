package com.balians.musicgen.provider.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;

public record SunoRecordInfoResultDto(
        @JsonAlias({"sunoData", "suno_data", "clips", "data", "songs"})
        List<SunoRecordInfoTrackDto> sunoData
) {
}
