package com.balians.musicgen.auth.repository;

import com.balians.musicgen.auth.model.InviteCode;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface InviteCodeRepository extends MongoRepository<InviteCode, String> {

    Optional<InviteCode> findByCode(String code);

    List<InviteCode> findAllByOrderByCreatedAtDesc();
}
