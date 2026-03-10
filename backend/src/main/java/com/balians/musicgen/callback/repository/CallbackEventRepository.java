package com.balians.musicgen.callback.repository;

import com.balians.musicgen.callback.model.CallbackEvent;
import com.balians.musicgen.common.enums.CallbackProcessingStatus;
import java.util.Collection;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CallbackEventRepository extends MongoRepository<CallbackEvent, String> {

    List<CallbackEvent> findByProviderTaskIdOrderByReceivedAtDesc(String providerTaskId);

    List<CallbackEvent> findByProcessingStatusOrderByReceivedAtAsc(CallbackProcessingStatus processingStatus);

    Optional<CallbackEvent> findFirstByProviderTaskIdAndCallbackTypeAndPayloadHashAndProcessingStatusInOrderByReceivedAtDesc(
            String providerTaskId,
            String callbackType,
            String payloadHash,
            Collection<CallbackProcessingStatus> processingStatuses
    );

    List<CallbackEvent> findByGenerationJobIdOrderByReceivedAtDesc(String generationJobId);

    long countByReceivedAtAfter(Instant receivedAt);
}
