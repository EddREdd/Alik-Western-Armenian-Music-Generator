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

    Page<GenerationJob> findByProjectIdAndHiddenFromLibraryFalse(String projectId, Pageable pageable);

    Page<GenerationJob> findByHiddenFromLibraryFalse(Pageable pageable);

    Page<GenerationJob> findByInternalStatus(InternalJobStatus internalJobStatus, Pageable pageable);

    Page<GenerationJob> findByInternalStatusAndHiddenFromLibraryFalse(InternalJobStatus internalJobStatus, Pageable pageable);

    Page<GenerationJob> findByProviderStatus(ProviderJobStatus providerJobStatus, Pageable pageable);

    Page<GenerationJob> findByProviderStatusAndHiddenFromLibraryFalse(ProviderJobStatus providerJobStatus, Pageable pageable);

    Page<GenerationJob> findByProjectIdAndInternalStatus(String projectId, InternalJobStatus internalJobStatus, Pageable pageable);

    Page<GenerationJob> findByProjectIdAndInternalStatusAndHiddenFromLibraryFalse(
            String projectId,
            InternalJobStatus internalJobStatus,
            Pageable pageable
    );

    Page<GenerationJob> findByOwnerUserIdAndHiddenFromLibraryFalse(String ownerUserId, Pageable pageable);

    Page<GenerationJob> findByOwnerUserIdAndProjectIdAndHiddenFromLibraryFalse(
            String ownerUserId,
            String projectId,
            Pageable pageable
    );

    Page<GenerationJob> findByOwnerUserIdAndInternalStatusAndHiddenFromLibraryFalse(
            String ownerUserId,
            InternalJobStatus internalJobStatus,
            Pageable pageable
    );

    Page<GenerationJob> findByOwnerUserIdAndProviderStatusAndHiddenFromLibraryFalse(
            String ownerUserId,
            ProviderJobStatus providerJobStatus,
            Pageable pageable
    );

    Page<GenerationJob> findByOwnerUserIdAndProjectIdAndInternalStatusAndHiddenFromLibraryFalse(
            String ownerUserId,
            String projectId,
            InternalJobStatus internalJobStatus,
            Pageable pageable
    );

    Page<GenerationJob> findByOwnerUserIdAndProjectIdAndProviderStatusAndHiddenFromLibraryFalse(
            String ownerUserId,
            String projectId,
            ProviderJobStatus providerJobStatus,
            Pageable pageable
    );

    List<GenerationJob> findByOwnerUserIdAndHiddenFromLibraryFalseOrderByCreatedAtDesc(String ownerUserId);

    List<GenerationJob> findByInternalStatusInAndProviderTaskIdIsNotNullOrderByNextPollAtAscCreatedAtAsc(
            List<InternalJobStatus> internalStatuses,
            Pageable pageable
    );
}
