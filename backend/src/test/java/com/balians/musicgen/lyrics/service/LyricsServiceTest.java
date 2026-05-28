package com.balians.musicgen.lyrics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.balians.musicgen.auth.model.UserAccount;
import com.balians.musicgen.common.exception.NotFoundException;
import com.balians.musicgen.lyrics.dto.CreateLyricRequest;
import com.balians.musicgen.lyrics.dto.UpdateLyricRequest;
import com.balians.musicgen.lyrics.model.LyricEntry;
import com.balians.musicgen.lyrics.model.LyricVersion;
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
class LyricsServiceTest {

    private static final String ARMENIAN_TITLE = "Երգ";
    private static final String ARMENIAN_BODY = "բ".repeat(50);

    @Mock
    private LyricEntryRepository lyricEntryRepository;
    @Mock
    private LyricTextValidator lyricTextValidator;
    @Mock
    private ReadyLibraryService readyLibraryService;

    @InjectMocks
    private LyricsService service;

    @Test
    void autoProjectId_withOwner_returnsUserScopedId() {
        assertThat(service.autoProjectId(UserAccount.builder().id("abc-123").build()))
                .isEqualTo("user-abc-123");
    }

    @Test
    void autoProjectId_withoutOwner_returnsSystem() {
        assertThat(service.autoProjectId(null)).isEqualTo("system");
        assertThat(service.autoProjectId(UserAccount.builder().id(null).build())).isEqualTo("system");
    }

    @Test
    void create_missingProjectId_usesAutoProjectId() {
        UserAccount userA = UserAccount.builder().id("user-a").build();
        when(lyricEntryRepository.save(any(LyricEntry.class))).thenAnswer(invocation -> {
            LyricEntry entry = invocation.getArgument(0);
            entry.setId("lyric-1");
            return entry;
        });

        service.create(new CreateLyricRequest(null, ARMENIAN_TITLE, ARMENIAN_BODY, null), userA);

        ArgumentCaptor<LyricEntry> captor = ArgumentCaptor.forClass(LyricEntry.class);
        verify(lyricEntryRepository).save(captor.capture());
        assertThat(captor.getValue().getProjectId()).isEqualTo("user-user-a");
        assertThat(captor.getValue().getUserId()).isEqualTo("user-a");
        assertThat(captor.getValue().getPublicReadyLibrary()).isFalse();
    }

    @Test
    void listForUser_returnsOnlyOwnerPrivateLyrics() {
        UserAccount userA = UserAccount.builder().id("user-a").build();
        LyricEntry entry = LyricEntry.builder()
                .id("lyric-1")
                .userId("user-a")
                .projectId("user-user-a")
                .title(ARMENIAN_TITLE)
                .body(ARMENIAN_BODY)
                .publicReadyLibrary(false)
                .build();
        when(lyricEntryRepository.findPrivateByUserIdOrderByUpdatedAtDesc("user-a"))
                .thenReturn(List.of(entry));

        var summaries = service.listForUser(userA, null);

        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).id()).isEqualTo("lyric-1");
        verify(lyricEntryRepository).findPrivateByUserIdOrderByUpdatedAtDesc("user-a");
    }

    @Test
    void getById_ownerCanAccessOwnLyric() {
        UserAccount userA = UserAccount.builder().id("user-a").build();
        LyricEntry entry = privateLyric("lyric-1", "user-a");
        when(lyricEntryRepository.findByIdAndUserId("lyric-1", "user-a")).thenReturn(Optional.of(entry));

        var response = service.getById("lyric-1", userA);

        assertThat(response.id()).isEqualTo("lyric-1");
        assertThat(response.userId()).isEqualTo("user-a");
    }

    @Test
    void getById_otherUserCannotAccessLyric() {
        UserAccount userB = UserAccount.builder().id("user-b").build();
        when(lyricEntryRepository.findByIdAndUserId("lyric-1", "user-b")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById("lyric-1", userB))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Lyric not found: lyric-1");
    }

    @Test
    void update_ownerCanUpdateOwnLyric() {
        UserAccount userA = UserAccount.builder().id("user-a").build();
        LyricEntry entry = privateLyric("lyric-1", "user-a");
        when(lyricEntryRepository.findByIdAndUserId("lyric-1", "user-a")).thenReturn(Optional.of(entry));
        when(lyricEntryRepository.save(any(LyricEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.update("lyric-1", new UpdateLyricRequest("Թարմված", ARMENIAN_BODY), userA);

        verify(lyricEntryRepository).save(entry);
    }

    @Test
    void update_otherUserCannotUpdateLyric() {
        UserAccount userB = UserAccount.builder().id("user-b").build();
        when(lyricEntryRepository.findByIdAndUserId("lyric-1", "user-b")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(
                "lyric-1",
                new UpdateLyricRequest("Թարմված", ARMENIAN_BODY),
                userB
        ))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Lyric not found: lyric-1");

        verify(lyricEntryRepository, never()).save(any());
    }

    @Test
    void delete_ownerCanDeleteOwnLyric() {
        UserAccount userA = UserAccount.builder().id("user-a").build();
        LyricEntry entry = privateLyric("lyric-1", "user-a");
        when(lyricEntryRepository.findByIdAndUserId("lyric-1", "user-a")).thenReturn(Optional.of(entry));

        service.delete("lyric-1", userA);

        verify(lyricEntryRepository).delete(entry);
    }

    @Test
    void delete_otherUserCannotDeleteLyric() {
        UserAccount userB = UserAccount.builder().id("user-b").build();
        when(lyricEntryRepository.findByIdAndUserId("lyric-1", "user-b")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete("lyric-1", userB))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Lyric not found: lyric-1");

        verify(lyricEntryRepository, never()).delete(any());
    }

    @Test
    void assertAvailableForGeneration_otherUserLyric_isRejected() {
        UserAccount userB = UserAccount.builder().id("user-b").build();
        when(lyricEntryRepository.findByIdAndUserId("lyric-1", "user-b")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assertAvailableForGeneration("lyric-1", userB))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Lyric not found: lyric-1");
    }

    @Test
    void getById_publicReadyLibraryLyric_isNotFoundForPrivateAccess() {
        UserAccount userA = UserAccount.builder().id("user-a").build();
        LyricEntry publicEntry = privateLyric("lyric-public", "user-a");
        publicEntry.setPublicReadyLibrary(true);
        when(lyricEntryRepository.findByIdAndUserId("lyric-public", "user-a")).thenReturn(Optional.of(publicEntry));

        assertThatThrownBy(() -> service.getById("lyric-public", userA))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Lyric not found: lyric-public");
    }

    @Test
    void restoreUnlockedVersion_mutatesSameLyric() {
        UserAccount userA = UserAccount.builder().id("user-a").build();
        LyricEntry entry = privateLyric("lyric-1", "user-a");
        entry.setTitle("Current");
        entry.setBody("բ".repeat(55));
        entry.setCurrentVersion(3);
        entry.setVersions(List.of(
                LyricVersion.builder().versionNumber(1).title("Old 1").body("բ".repeat(50)).editedAt(Instant.now()).build(),
                LyricVersion.builder().versionNumber(2).title("Old 2").body("բ".repeat(52)).editedAt(Instant.now()).build()
        ));
        when(lyricEntryRepository.findByIdAndUserId("lyric-1", "user-a")).thenReturn(Optional.of(entry));
        when(lyricEntryRepository.save(any(LyricEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var restored = service.restoreVersion("lyric-1", 2, userA);

        assertThat(restored.id()).isEqualTo("lyric-1");
        assertThat(restored.title()).isEqualTo("Old 2");
        assertThat(restored.currentVersion()).isEqualTo(4);
        assertThat(restored.versions()).hasSize(3);
        assertThat(restored.versions().get(2).title()).isEqualTo("Current");
    }

    @Test
    void restoreLockedVersion_createsNewPrivateCopy() {
        UserAccount userA = UserAccount.builder().id("user-a").build();
        LyricEntry locked = privateLyric("lyric-locked", "user-a");
        locked.setLocked(true);
        locked.setProjectId("user-user-a");
        locked.setLanguage("WESTERN_ARMENIAN");
        locked.setVersions(List.of(
                LyricVersion.builder().versionNumber(1).title("Base").body("բ".repeat(50)).editedAt(Instant.now()).build()
        ));
        when(lyricEntryRepository.findByIdAndUserId("lyric-locked", "user-a")).thenReturn(Optional.of(locked));
        when(lyricEntryRepository.save(any(LyricEntry.class))).thenAnswer(invocation -> {
            LyricEntry saved = invocation.getArgument(0);
            saved.setId("new-copy-1");
            return saved;
        });

        var restored = service.restoreVersion("lyric-locked", 1, userA);

        assertThat(restored.id()).isEqualTo("new-copy-1");
        assertThat(restored.title()).isEqualTo("Base (Restored v1)");
        assertThat(restored.locked()).isFalse();
        assertThat(restored.currentVersion()).isEqualTo(1);
        assertThat(restored.projectId()).isEqualTo("user-user-a");
    }

    @Test
    void anotherUserCannotRestoreSomeoneElsesLyricVersion() {
        UserAccount userB = UserAccount.builder().id("user-b").build();
        when(lyricEntryRepository.findByIdAndUserId("lyric-1", "user-b")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.restoreVersion("lyric-1", 1, userB))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Lyric not found: lyric-1");
    }

    private static LyricEntry privateLyric(String id, String userId) {
        return LyricEntry.builder()
                .id(id)
                .userId(userId)
                .projectId("user-" + userId)
                .title(ARMENIAN_TITLE)
                .body(ARMENIAN_BODY)
                .currentVersion(1)
                .locked(false)
                .publicReadyLibrary(false)
                .build();
    }
}
