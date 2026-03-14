package com.balians.musicgen.auth.model;

import com.balians.musicgen.common.audit.AuditableDocument;
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
@Document(collection = "user_accounts")
@CompoundIndexes({
        @CompoundIndex(name = "idx_user_accounts_email_verified", def = "{'email': 1, 'emailVerified': 1}"),
        @CompoundIndex(name = "idx_user_accounts_google_subject", def = "{'googleSubject': 1}", unique = true, sparse = true)
})
public class UserAccount extends AuditableDocument {

    @Id
    private String id;

    @Indexed(name = "idx_user_accounts_email", unique = true)
    private String email;

    private String passwordHash;
    private Boolean emailVerified;
    private String pendingEmail;
    private String googleSubject;
    private String googleEmail;
    private Boolean admin;
    private Boolean unlimitedCredits;
    private Integer creditsRemaining;
    private Integer creditsUsed;
    private Integer songsGenerated;
    private Boolean frozen;
    private java.time.Instant frozenAt;
    private String freezeReason;
}
