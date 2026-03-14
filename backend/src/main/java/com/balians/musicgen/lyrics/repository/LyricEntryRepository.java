package com.balians.musicgen.lyrics.repository;

import com.balians.musicgen.lyrics.model.LyricEntry;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface LyricEntryRepository extends MongoRepository<LyricEntry, String> {

    List<LyricEntry> findByProjectIdOrderByUpdatedAtDesc(String projectId);
}
