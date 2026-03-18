package com.balians.musicgen.admin.model;

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
@Document(collection = "invite_code_email_send_events")
@CompoundIndexes({
        @CompoundIndex(name = "idx_invite_email_event_invite_sent", def = "{'inviteCodeId': 1, 'sentAt': -1}")
})
public class InviteCodeEmailSendEvent extends AuditableDocument {

    @Id
    private String id;

    @Indexed(name = "idx_invite_email_event_invite")
    private String inviteCodeId;

    private String code;
    private String email;
    private Instant sentAt;
}
