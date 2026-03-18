package com.balians.musicgen.generation.repository;

import com.balians.musicgen.generation.model.GenerationTrack;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GenerationTrackRepository extends MongoRepository<GenerationTrack, String> {

    List<GenerationTrack> findByGenerationJobIdOrderByTrackIndexAsc(String generationJobId);

    void deleteByGenerationJobId(String generationJobId);

    Optional<GenerationTrack> findByProviderMusicId(String providerMusicId);

    Optional<GenerationTrack> findByGenerationJobIdAndProviderMusicId(String generationJobId, String providerMusicId);

    List<GenerationTrack> findByGenerationJobIdAndProviderMusicIdOrderByCreatedAtDesc(
            String generationJobId,
            String providerMusicId
    );
}
