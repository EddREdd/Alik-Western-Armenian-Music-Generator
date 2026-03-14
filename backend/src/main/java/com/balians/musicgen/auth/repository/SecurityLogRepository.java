package com.balians.musicgen.auth.repository;

import com.balians.musicgen.auth.model.SecurityLog;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SecurityLogRepository extends MongoRepository<SecurityLog, String> {

    List<SecurityLog> findByUserIdOrderByOccurredAtDesc(String userId);
}
