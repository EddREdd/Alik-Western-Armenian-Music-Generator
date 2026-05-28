package com.balians.musicgen.lyrics.repository;

import com.balians.musicgen.lyrics.model.LyricEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface LyricEntryRepository extends MongoRepository<LyricEntry, String> {

    List<LyricEntry> findByProjectIdOrderByUpdatedAtDesc(String projectId);

    @Query("{ 'userId': ?0, 'publicReadyLibrary': { $ne: true } }")
    List<LyricEntry> findPrivateByUserIdOrderByUpdatedAtDesc(String userId);

    @Query("{ 'userId': ?0, 'projectId': ?1, 'publicReadyLibrary': { $ne: true } }")
    List<LyricEntry> findPrivateByUserIdAndProjectIdOrderByUpdatedAtDesc(String userId, String projectId);

    Optional<LyricEntry> findByIdAndUserId(String id, String userId);

    List<LyricEntry> findByPublicReadyLibraryTrueOrderByUpdatedAtDesc();

    List<LyricEntry> findByPublicReadyLibraryTrueAndLanguageOrderByUpdatedAtDesc(String language);

    Optional<LyricEntry> findByIdAndPublicReadyLibraryTrue(String id);
}
