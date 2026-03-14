package com.balians.musicgen.lyrics.model;

import com.balians.musicgen.common.audit.AuditableDocument;
import java.util.ArrayList;
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
@Document(collection = "lyrics_entries")
@CompoundIndexes({
        @CompoundIndex(name = "idx_lyrics_project_updated_at", def = "{'projectId': 1, 'updatedAt': -1}"),
        @CompoundIndex(name = "idx_lyrics_project_locked_updated_at", def = "{'projectId': 1, 'locked': 1, 'updatedAt': -1}")
})
public class LyricEntry extends AuditableDocument {

    @Id
    private String id;

    @Indexed(name = "idx_lyrics_user_id", sparse = true)
    private String userId;

    @Indexed(name = "idx_lyrics_project_id")
    private String projectId;

    private String title;
    private String body;
    private Integer currentVersion;
    private Boolean locked;

    @Builder.Default
    private List<String> linkedSongIds = new ArrayList<>();

    @Builder.Default
    private List<LyricVersion> versions = new ArrayList<>();
}
