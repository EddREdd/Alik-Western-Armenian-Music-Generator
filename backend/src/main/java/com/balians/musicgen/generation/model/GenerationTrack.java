package com.balians.musicgen.generation.model;

import com.balians.musicgen.common.audit.AuditableDocument;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "generation_tracks")
@CompoundIndexes({
        @CompoundIndex(name = "idx_generation_tracks_job_track_index", def = "{'generationJobId': 1, 'trackIndex': 1}"),
        @CompoundIndex(name = "idx_generation_tracks_job_provider_music", def = "{'generationJobId': 1, 'providerMusicId': 1}")
})
public class GenerationTrack extends AuditableDocument {

    @Id
    private String id;

    @Indexed(name = "idx_generation_tracks_owner_user_id", sparse = true)
    private String ownerUserId;

    @Indexed(name = "idx_generation_tracks_job_id")
    private String generationJobId;

    private String projectId;
    private String lyricId;
    private String lyricTitle;
    private String lyricText;
    @Indexed(name = "idx_generation_tracks_provider_music_id", sparse = true)
    private String providerMusicId;

    private Integer trackIndex;
    private String audioUrl;
    private String streamAudioUrl;
    private String imageUrl;
    private String localAudioPath;
    private String localAudioUrl;
    private String localImagePath;
    private String localImageUrl;
    private String lyricsOrPrompt;
    private String modelName;
    private String title;
    private List<String> tags;
    private Integer durationSeconds;
    private Instant providerCreateTime;
    private Instant assetExpiryAt;
    private Boolean selectedFlag;
}
