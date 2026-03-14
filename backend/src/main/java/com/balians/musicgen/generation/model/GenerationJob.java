package com.balians.musicgen.generation.model;

import com.balians.musicgen.common.audit.AuditableDocument;
import com.balians.musicgen.common.enums.GenerationModel;
import com.balians.musicgen.common.enums.InternalJobStatus;
import com.balians.musicgen.common.enums.JobSourceType;
import com.balians.musicgen.common.enums.ProviderJobStatus;
import java.time.Instant;
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
@Document(collection = "generation_jobs")
@CompoundIndexes({
        @CompoundIndex(name = "idx_generation_jobs_project_created_at", def = "{'projectId': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "idx_generation_jobs_internal_status_created_at", def = "{'internalStatus': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "idx_generation_jobs_provider_status_created_at", def = "{'providerStatus': 1, 'createdAt': -1}")
})
public class GenerationJob extends AuditableDocument {

    @Id
    private String id;

    @Indexed(name = "idx_generation_jobs_owner_user_id", sparse = true)
    private String ownerUserId;

    @Indexed(name = "idx_generation_jobs_project_id")
    private String projectId;

    private String templateId;
    private String lyricId;
    private String lyricTitle;
    private JobSourceType sourceType;

    @Indexed(name = "idx_generation_jobs_internal_status")
    private InternalJobStatus internalStatus;

    @Indexed(name = "idx_generation_jobs_provider_status")
    private ProviderJobStatus providerStatus;

    @Indexed(name = "idx_generation_jobs_provider_task_id", sparse = true)
    private String providerTaskId;

    private String promptFinal;
    private String styleFinal;
    private String titleFinal;
    private Boolean customMode;
    private Boolean instrumental;
    private GenerationModel model;
    private String errorCode;
    private String errorMessage;
    private Instant submittedAt;
    private Instant completedAt;
    private Instant failedAt;
    private Instant userDeletedAt;
    private Boolean hiddenFromLibrary;
    @Indexed(name = "idx_generation_jobs_next_poll_at", sparse = true)
    private Instant nextPollAt;
    private Instant lastPolledAt;
    private Integer pollAttemptCount;

    @Builder.Default
    private List<JobStatusHistoryEntry> statusHistory = new ArrayList<>();
}
