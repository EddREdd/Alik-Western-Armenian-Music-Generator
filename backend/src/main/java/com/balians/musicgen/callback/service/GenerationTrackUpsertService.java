package com.balians.musicgen.callback.service;

import com.balians.musicgen.callback.dto.SunoCallbackTrackDto;
import com.balians.musicgen.generation.model.GenerationJob;
import com.balians.musicgen.generation.model.GenerationTrack;
import com.balians.musicgen.generation.repository.GenerationTrackRepository;
import com.balians.musicgen.media.service.TrackMediaStorageService;
import com.balians.musicgen.provider.dto.SunoRecordInfoTrackDto;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationTrackUpsertService {

    private final GenerationTrackRepository generationTrackRepository;
    private final TrackMediaStorageService trackMediaStorageService;

    public int upsertTracks(GenerationJob job, List<SunoCallbackTrackDto> tracks) {
        if (tracks == null || tracks.isEmpty()) {
            return 0;
        }

        int upsertedCount = 0;

        for (int index = 0; index < tracks.size(); index++) {
            SunoCallbackTrackDto trackDto = tracks.get(index);
            if (trackDto == null || isBlank(trackDto.id())) {
                log.warn("Skipping callback track without provider id for jobId={}", job.getId());
                continue;
            }

            GenerationTrack track = generationTrackRepository
                    .findByGenerationJobIdAndProviderMusicId(job.getId(), trackDto.id().trim())
                    .orElseGet(() -> GenerationTrack.builder()
                            .ownerUserId(job.getOwnerUserId())
                            .generationJobId(job.getId())
                            .projectId(job.getProjectId())
                            .lyricId(job.getLyricId())
                            .lyricTitle(job.getLyricTitle())
                            .lyricText(job.getPromptFinal())
                            .providerMusicId(trackDto.id().trim())
                            .build());

            applyTrackFields(
                    track,
                    index + 1,
                    trackDto.audioUrl(),
                    trackDto.streamAudioUrl(),
                    trackDto.imageUrl(),
                    trackDto.prompt(),
                    trackDto.modelName(),
                    trackDto.title(),
                    trackDto.tags(),
                    trackDto.duration(),
                    trackDto.createTime()
            );
            trackMediaStorageService.storeTrackAssets(track);
            generationTrackRepository.save(track);
            upsertedCount++;
        }

        log.info("Upserted {} track(s) for generation job id={}", upsertedCount, job.getId());
        return upsertedCount;
    }

    public int upsertProviderTracks(GenerationJob job, List<SunoRecordInfoTrackDto> tracks) {
        if (tracks == null || tracks.isEmpty()) {
            return 0;
        }

        int upsertedCount = 0;

        for (int index = 0; index < tracks.size(); index++) {
            SunoRecordInfoTrackDto trackDto = tracks.get(index);
            if (trackDto == null || isBlank(trackDto.id())) {
                log.warn("Skipping polled track without provider id for jobId={}", job.getId());
                continue;
            }

            GenerationTrack track = generationTrackRepository
                    .findByGenerationJobIdAndProviderMusicId(job.getId(), trackDto.id().trim())
                    .orElseGet(() -> GenerationTrack.builder()
                            .ownerUserId(job.getOwnerUserId())
                            .generationJobId(job.getId())
                            .projectId(job.getProjectId())
                            .lyricId(job.getLyricId())
                            .lyricTitle(job.getLyricTitle())
                            .lyricText(job.getPromptFinal())
                            .providerMusicId(trackDto.id().trim())
                            .build());

            applyTrackFields(
                    track,
                    index + 1,
                    trackDto.audioUrl(),
                    trackDto.streamAudioUrl(),
                    trackDto.imageUrl(),
                    trackDto.prompt(),
                    trackDto.modelName(),
                    trackDto.title(),
                    trackDto.tags(),
                    trackDto.duration(),
                    trackDto.createTime()
            );
            trackMediaStorageService.storeTrackAssets(track);
            generationTrackRepository.save(track);
            upsertedCount++;
        }

        log.info("Upserted {} polled track(s) for generation job id={}", upsertedCount, job.getId());
        return upsertedCount;
    }

    private void applyTrackFields(
            GenerationTrack track,
            int trackIndex,
            String audioUrl,
            String streamAudioUrl,
            String imageUrl,
            String prompt,
            String modelName,
            String title,
            String tags,
            Integer duration,
            String createTime
    ) {
        track.setTrackIndex(trackIndex);
        track.setAudioUrl(trimToNull(audioUrl));
        track.setStreamAudioUrl(trimToNull(streamAudioUrl));
        track.setImageUrl(trimToNull(imageUrl));
        track.setLyricsOrPrompt(trimToNull(prompt));
        track.setModelName(trimToNull(modelName));
        track.setTitle(trimToNull(title));
        track.setTags(parseTags(tags));
        track.setDurationSeconds(duration);
        track.setProviderCreateTime(parseInstant(createTime));
        track.setSelectedFlag(Boolean.FALSE);
    }

    private List<String> parseTags(String tags) {
        if (isBlank(tags)) {
            return Collections.emptyList();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .toList();
    }

    private Instant parseInstant(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value.trim()).toInstant();
        } catch (DateTimeParseException ignored) {
            log.warn("Unable to parse provider track createTime='{}'", value);
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        if (isBlank(value)) {
            return null;
        }
        return value.trim();
    }
}
