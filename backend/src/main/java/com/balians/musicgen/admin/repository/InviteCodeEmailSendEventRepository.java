package com.balians.musicgen.admin.repository;

import com.balians.musicgen.admin.model.InviteCodeEmailSendEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface InviteCodeEmailSendEventRepository extends MongoRepository<InviteCodeEmailSendEvent, String> {

    void deleteByInviteCodeId(String inviteCodeId);
}
