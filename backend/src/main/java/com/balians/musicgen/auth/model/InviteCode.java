package com.balians.musicgen.auth.model;

import com.balians.musicgen.common.audit.AuditableDocument;
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
@Document(collection = "invite_codes")
public class InviteCode extends AuditableDocument {

    @Id
    private String id;

    @Indexed(name = "idx_invite_codes_code", unique = true)
    private String code;

    private Boolean active;
    private String usedByUserId;
    private Instant usedAt;
    private String lastSentToEmail;
    private Instant lastSentAt;
}
