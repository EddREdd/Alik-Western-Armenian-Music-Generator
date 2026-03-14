package com.balians.musicgen.auth.repository;

import com.balians.musicgen.auth.model.UserAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserAccountRepository extends MongoRepository<UserAccount, String> {

    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findByGoogleSubject(String googleSubject);

    long countByEmailVerifiedTrue();

    List<UserAccount> findAllByOrderByCreatedAtDesc();
}
