package com.balians.musicgen.auth.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_sessions")
public class UserSession {

    @Id
    private String id;

    @Indexed(name = "idx_user_sessions_user_id")
    private String userId;

    @Indexed(name = "idx_user_sessions_token", unique = true)
    private String token;

    private Instant expiresAt;
    private Instant lastUsedAt;
    private Instant createdAt;
}
