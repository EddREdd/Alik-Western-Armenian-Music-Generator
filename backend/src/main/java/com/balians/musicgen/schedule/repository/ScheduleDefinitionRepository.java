package com.balians.musicgen.schedule.repository;

import com.balians.musicgen.schedule.model.ScheduleDefinition;
import java.time.Instant;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ScheduleDefinitionRepository extends MongoRepository<ScheduleDefinition, String> {

    List<ScheduleDefinition> findByEnabledTrueOrderByCreatedAtDesc();

    List<ScheduleDefinition> findByEnabledTrueAndNextRunAtLessThanEqualOrderByNextRunAtAsc(Instant nextRunAt);
}
