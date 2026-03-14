package com.balians.musicgen.lyrics.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LyricVersion {

    private Integer versionNumber;
    private String title;
    private String body;
    private Instant editedAt;
}
