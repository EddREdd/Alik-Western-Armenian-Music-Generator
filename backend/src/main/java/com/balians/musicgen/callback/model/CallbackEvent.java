package com.balians.musicgen.callback.model;

import com.balians.musicgen.common.enums.CallbackProcessingStatus;
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
@Document(collection = "callback_events")
@CompoundIndexes({
        @CompoundIndex(name = "idx_callback_events_task_received_at", def = "{'providerTaskId': 1, 'receivedAt': -1}"),
        @CompoundIndex(name = "idx_callback_events_task_type_hash", def = "{'providerTaskId': 1, 'callbackType': 1, 'payloadHash': 1}")
})
public class CallbackEvent {

    @Id
    private String id;

    @Indexed(name = "idx_callback_events_provider_task_id")
    private String providerTaskId;

    private String generationJobId;
    private String callbackType;
    private String callbackCode;
    private String message;
    private String payloadJson;

    @Indexed(name = "idx_callback_events_payload_hash", sparse = true)
    private String payloadHash;

    @Indexed(name = "idx_callback_events_received_at")
    private Instant receivedAt;

    private Instant processedAt;

    @Indexed(name = "idx_callback_events_processing_status")
    private CallbackProcessingStatus processingStatus;

    private String processingError;
}
