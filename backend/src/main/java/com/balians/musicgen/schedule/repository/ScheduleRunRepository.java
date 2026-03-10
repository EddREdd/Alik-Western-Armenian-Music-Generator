package com.balians.musicgen.schedule.repository;

import com.balians.musicgen.schedule.model.ScheduleRun;
import com.balians.musicgen.common.enums.ScheduleRunStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ScheduleRunRepository extends MongoRepository<ScheduleRun, String> {

    List<ScheduleRun> findByRunDate(LocalDate runDate);

    List<ScheduleRun> findByScheduleDefinitionIdOrderByStartedAtDesc(String scheduleDefinitionId);

    Optional<ScheduleRun> findFirstByScheduleDefinitionIdAndRunDateOrderByStartedAtDesc(String scheduleDefinitionId, LocalDate runDate);

    List<ScheduleRun> findByStatusOrderByStartedAtDesc(ScheduleRunStatus status);

    long countByStatusAndStartedAtAfter(ScheduleRunStatus status, Instant startedAt);
}
