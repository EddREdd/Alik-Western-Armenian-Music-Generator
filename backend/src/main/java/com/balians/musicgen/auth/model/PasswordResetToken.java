package com.balians.musicgen.auth.model;

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
@Document(collection = "password_reset_tokens")
@CompoundIndexes({
        @CompoundIndex(name = "idx_password_reset_email_token", def = "{'email': 1, 'token': 1}", unique = true),
        @CompoundIndex(name = "idx_password_reset_user_id", def = "{'userId': 1}")
})
public class PasswordResetToken extends AuditableDocument {

    @Id
    private String id;

    private String userId;

    @Indexed(name = "idx_password_reset_email")
    private String email;

    private String token;
    private Instant expiresAt;
    private Instant consumedAt;
}
