package com.balians.musicgen.lyrics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.balians.musicgen.admin.dto.CreateReadyLibraryLyricRequest;
import com.balians.musicgen.admin.dto.UpdateReadyLibraryLyricRequest;
import com.balians.musicgen.lyrics.model.LyricEntry;
import com.balians.musicgen.lyrics.repository.LyricEntryRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReadyLibraryServiceTest {

    @Mock
    private LyricEntryRepository lyricEntryRepository;
    @Mock
    private LyricTextValidator lyricTextValidator;

    @InjectMocks
    private ReadyLibraryService service;

    @Test
    void adminCanCreatePublicReadyLibraryLyric() {
        when(lyricEntryRepository.save(any(LyricEntry.class))).thenAnswer(invocation -> {
            LyricEntry entry = invocation.getArgument(0);
            entry.setId("ready-1");
            return entry;
        });

        var created = service.create("admin-1", new CreateReadyLibraryLyricRequest("My Title", "Hello world lyrics", "ENGLISH"));

        ArgumentCaptor<LyricEntry> captor = ArgumentCaptor.forClass(LyricEntry.class);
        verify(lyricEntryRepository).save(captor.capture());
        LyricEntry saved = captor.getValue();
        assertThat(saved.getPublicReadyLibrary()).isTrue();
        assertThat(saved.getUserId()).isNull();
        assertThat(saved.getProjectId()).isEqualTo("ready-library");
        assertThat(saved.getCreatedByAdminUserId()).isEqualTo("admin-1");
        assertThat(saved.getCurrentVersion()).isEqualTo(1);
        assertThat(saved.getLocked()).isFalse();
        assertThat(created.id()).isEqualTo("ready-1");
    }

    @Test
    void adminCanUpdateReadyLibraryLyricAndVersionIsStored() {
        LyricEntry entry = LyricEntry.builder()
                .id("ready-1")
                .title("Old")
                .body("Old body")
                .language("ENGLISH")
                .publicReadyLibrary(true)
                .projectId("ready-library")
                .currentVersion(1)
                .versions(List.of())
                .build();
        when(lyricEntryRepository.findByIdAndPublicReadyLibraryTrue("ready-1")).thenReturn(Optional.of(entry));
        when(lyricEntryRepository.save(any(LyricEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var updated = service.update("ready-1", new UpdateReadyLibraryLyricRequest("New", "New body with text", "ENGLISH"));

        assertThat(updated.currentVersion()).isEqualTo(2);
        assertThat(updated.versions()).hasSize(1);
        assertThat(updated.versions().get(0).versionNumber()).isEqualTo(1);
        assertThat(updated.versions().get(0).title()).isEqualTo("Old");
    }

    @Test
    void adminCanDeleteReadyLibraryLyric() {
        LyricEntry entry = LyricEntry.builder().id("ready-1").publicReadyLibrary(true).build();
        when(lyricEntryRepository.findByIdAndPublicReadyLibraryTrue("ready-1")).thenReturn(Optional.of(entry));

        service.delete("ready-1");

        verify(lyricEntryRepository).delete(entry);
    }

    @Test
    void publicReadyLibraryListReturnsOnlyPublicLyrics() {
        LyricEntry publicLyric = LyricEntry.builder()
                .id("ready-1")
                .title("Public")
                .body("Some body")
                .language("ENGLISH")
                .publicReadyLibrary(true)
                .currentVersion(1)
                .build();
        when(lyricEntryRepository.findByPublicReadyLibraryTrueOrderByUpdatedAtDesc()).thenReturn(List.of(publicLyric));

        var list = service.listPublic(null, null);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).publicReadyLibrary()).isTrue();
    }

    @Test
    void publicReadyLibraryCanFilterByLanguage() {
        LyricEntry english = LyricEntry.builder()
                .id("ready-en")
                .title("English")
                .body("[Verse] hello")
                .language("ENGLISH")
                .publicReadyLibrary(true)
                .currentVersion(1)
                .build();
        when(lyricEntryRepository.findByPublicReadyLibraryTrueAndLanguageOrderByUpdatedAtDesc("ENGLISH"))
                .thenReturn(List.of(english));

        var list = service.listPublic(null, "ENGLISH");

        assertThat(list).hasSize(1);
        assertThat(list.get(0).language()).isEqualTo("ENGLISH");
    }

    @Test
    void privateUserLyricsDoNotAppearInReadyLibrary() {
        when(lyricEntryRepository.findByPublicReadyLibraryTrueOrderByUpdatedAtDesc()).thenReturn(List.of());

        var list = service.listPublic(null, null);

        assertThat(list).isEmpty();
    }

}