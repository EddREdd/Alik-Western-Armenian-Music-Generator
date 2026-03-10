package com.balians.musicgen.polling.model;

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
@Document(collection = "poll_attempts")
@CompoundIndexes({
        @CompoundIndex(name = "idx_poll_attempts_job_requested_at", def = "{'generationJobId': 1, 'requestedAt': -1}"),
        @CompoundIndex(name = "idx_poll_attempts_task_requested_at", def = "{'providerTaskId': 1, 'requestedAt': -1}")
})
public class PollAttempt {

    @Id
    private String id;

    @Indexed(name = "idx_poll_attempts_generation_job_id")
    private String generationJobId;

    @Indexed(name = "idx_poll_attempts_provider_task_id")
    private String providerTaskId;

    private Integer attemptNo;
    @Indexed(name = "idx_poll_attempts_requested_at")
    private Instant requestedAt;
    private String providerStatus;
    private Integer responseCode;
    private String responseJson;
    @Indexed(name = "idx_poll_attempts_next_poll_at", sparse = true)
    private Instant nextPollAt;
    private PollAttemptOutcome outcome;
    private String errorMessage;
}
