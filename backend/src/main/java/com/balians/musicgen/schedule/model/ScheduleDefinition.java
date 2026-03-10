package com.balians.musicgen.schedule.model;

import com.balians.musicgen.common.audit.AuditableDocument;
import java.time.Instant;
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
@Document(collection = "schedule_definitions")
@CompoundIndexes({
        @CompoundIndex(name = "idx_schedule_definitions_enabled_next_run", def = "{'enabled': 1, 'nextRunAt': 1}"),
        @CompoundIndex(name = "idx_schedule_definitions_project_next_run", def = "{'projectId': 1, 'nextRunAt': 1}")
})
public class ScheduleDefinition extends AuditableDocument {

    @Id
    private String id;

    @Indexed(name = "idx_schedule_definitions_project_id")
    private String projectId;

    private String templateId;
    private String name;
    private String timezone;
    private String cronExpression;
    private Boolean enabled;
    private Boolean autoSubmitToProvider;
    private Integer creditsMinThreshold;
    private Instant lastRunAt;
    @Indexed(name = "idx_schedule_definitions_next_run_at", sparse = true)
    private Instant nextRunAt;
}
