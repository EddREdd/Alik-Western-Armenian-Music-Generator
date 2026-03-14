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
@Document(collection = "otp_codes")
@CompoundIndexes({
        @CompoundIndex(name = "idx_otp_codes_email_purpose", def = "{'email': 1, 'purpose': 1, 'createdAt': -1}")
})
public class OtpCode extends AuditableDocument {

    @Id
    private String id;

    private String userId;

    @Indexed(name = "idx_otp_codes_email")
    private String email;

    private OtpPurpose purpose;
    private String code;
    private Instant expiresAt;
    private Instant consumedAt;
}
