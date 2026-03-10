package com.balians.musicgen.polling.repository;

import com.balians.musicgen.polling.model.PollAttempt;
import java.time.Instant;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PollAttemptRepository extends MongoRepository<PollAttempt, String> {

    List<PollAttempt> findByGenerationJobIdOrderByRequestedAtDesc(String generationJobId);

    long countByRequestedAtAfter(Instant requestedAt);
}
