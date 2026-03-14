package com.balians.musicgen.auth.model;

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
@Document(collection = "security_logs")
@CompoundIndexes({
        @CompoundIndex(name = "idx_security_logs_user_event", def = "{'userId': 1, 'eventType': 1, 'occurredAt': -1}")
})
public class SecurityLog {

    @Id
    private String id;

    @Indexed(name = "idx_security_logs_user_id")
    private String userId;

    private String eventType;
    private String email;
    private String details;
    private Instant occurredAt;
}
