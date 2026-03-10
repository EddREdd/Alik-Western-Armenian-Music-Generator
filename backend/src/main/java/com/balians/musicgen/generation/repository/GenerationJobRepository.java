package com.balians.musicgen.generation.repository;

import com.balians.musicgen.common.enums.InternalJobStatus;
import com.balians.musicgen.common.enums.ProviderJobStatus;
import com.balians.musicgen.generation.model.GenerationJob;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GenerationJobRepository extends MongoRepository<GenerationJob, String> {

    Optional<GenerationJob> findByProviderTaskId(String providerTaskId);

    Page<GenerationJob> findByProjectId(String projectId, Pageable pageable);

    Page<GenerationJob> findByInternalStatus(InternalJobStatus internalJobStatus, Pageable pageable);

    Page<GenerationJob> findByProviderStatus(ProviderJobStatus providerJobStatus, Pageable pageable);

    Page<GenerationJob> findByProjectIdAndInternalStatus(String projectId, InternalJobStatus internalJobStatus, Pageable pageable);

    List<GenerationJob> findByInternalStatusInAndProviderTaskIdIsNotNullOrderByNextPollAtAscCreatedAtAsc(
            List<InternalJobStatus> internalStatuses,
            Pageable pageable
    );
}
