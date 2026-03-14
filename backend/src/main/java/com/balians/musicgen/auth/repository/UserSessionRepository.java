package com.balians.musicgen.auth.repository;

import com.balians.musicgen.auth.model.UserSession;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserSessionRepository extends MongoRepository<UserSession, String> {

    Optional<UserSession> findByToken(String token);

    List<UserSession> findByUserId(String userId);

    void deleteByUserId(String userId);
}
