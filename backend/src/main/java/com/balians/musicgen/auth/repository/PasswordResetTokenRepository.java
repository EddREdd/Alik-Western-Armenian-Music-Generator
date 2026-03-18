package com.balians.musicgen.auth.repository;

import com.balians.musicgen.auth.model.PasswordResetToken;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PasswordResetTokenRepository extends MongoRepository<PasswordResetToken, String> {

    Optional<PasswordResetToken> findByEmailAndToken(String email, String token);

    void deleteByEmail(String email);
}
