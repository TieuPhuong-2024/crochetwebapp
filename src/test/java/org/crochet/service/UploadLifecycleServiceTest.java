package org.crochet.service;

import org.crochet.model.File;
import org.crochet.model.UploadAsset;
import org.crochet.properties.R2Properties;
import org.crochet.repository.UploadAssetRepository;
import org.crochet.util.UploadReferenceParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.crochet.service.UploadTestSupport.*;

class UploadLifecycleServiceTest {
    private UploadAssetRepository repository;
    private R2Storage storage;
    private R2Properties properties;
    private UploadLifecycleService service;
    private UploadAsset asset;

    @BeforeEach
    void setUp() {
        repository = mock(UploadAssetRepository.class);
        storage = mock(R2Storage.class);
        properties = properties();
        service = new UploadLifecycleService(repository, storage, properties, new UploadReferenceParser(properties), CLOCK);
        asset = asset();
        when(repository.lockByObjectKey(KEY)).thenReturn(Optional.of(asset));
        when(storage.head("unit-test-bucket", KEY)).thenReturn(new R2Storage.Metadata(100, "image/png"));
        authenticate("owner", "ROLE_USER");
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void confirmsMetadataAndAttachesFiles() {
        File file = new File();
        file.setFileName(KEY);
        file.setFileContent(CDN + "/" + KEY);
        service.attach(new UploadLifecycleService.References().files(List.of(file)));
        assertThat(asset.getState()).isEqualTo(UploadAsset.State.ATTACHED);
        verify(storage).head("unit-test-bucket", KEY);
    }

    @Test
    void rejectsMismatchedFileNameAndUrlEvenWithExternalUrl() {
        File file = new File();
        file.setFileName(KEY);
        file.setFileContent("https://external.example.test/not-an-upload.png");
        assertThatThrownBy(() -> service.attach(new UploadLifecycleService.References().files(List.of(file))))
                .isInstanceOf(ResponseStatusException.class);
        file.setFileName("fake.png");
        file.setFileContent(CDN + "/" + KEY);
        assertThatThrownBy(() -> service.attach(new UploadLifecycleService.References().files(List.of(file))))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(storage);
    }

    @Test
    void parsesEntitiesEscapedPathsQueryFragmentAndDeduplicates() {
        String encodedKey = KEY.replace("/", "%2F");
        service.attach(new UploadLifecycleService.References()
                .urls(CDN.replace("/", "\\/") + "/" + encodedKey + "?x=1&amp;y=2#preview", CDN + "/" + KEY));
        assertThat(asset.getState()).isEqualTo(UploadAsset.State.ATTACHED);
        verify(repository, times(1)).lockByObjectKey(KEY);
        verify(storage, times(1)).head("unit-test-bucket", KEY);
    }

    @Test
    void htmlExtractsActualAttributesNotCommentOrScriptText() {
        String secondKey = KEY.replace("0001.png", "0002.png");
        var second = asset(secondKey, NOW.plusSeconds(1000));
        when(repository.lockByObjectKey(secondKey)).thenReturn(Optional.of(second));
        when(storage.head("unit-test-bucket", secondKey)).thenReturn(new R2Storage.Metadata(100, "image/png"));
        String first = CDN + "/" + KEY;
        String next = CDN + "/" + secondKey;
        service.attach(new UploadLifecycleService.References().content("""
                <picture><source srcset="%s 1x, %s 2x"><img src="%s?x=1&amp;y=2"></picture>
                <a href="%s#download">file</a><video poster="%s"><source src="%s"></video>
                <div style="background-image:url('%s')"></div>
                <!-- <img src="https://cdn.example.test/development/ignored.png"> -->
                <script>const url = 'https://cdn.example.test/development/ignored.png';</script>
                """.formatted(first, next, first, next, first, next, next)));
        assertThat(asset.getState()).isEqualTo(UploadAsset.State.ATTACHED);
        assertThat(second.getState()).isEqualTo(UploadAsset.State.ATTACHED);
        var order = inOrder(repository);
        order.verify(repository).lockByObjectKey(KEY);
        order.verify(repository).lockByObjectKey(secondKey);
        verify(repository, never()).lockByObjectKey("development/ignored.png");
    }

    @Test
    void preservesExternalFirebaseAndUntrackedLegacyWithoutEnrollment() {
        service.attach(new UploadLifecycleService.References()
                .urls("https://firebasestorage.googleapis.com/v0/b/old/o/file.png", "https://external.example.test/a.png",
                        CDN + "/legacy.png", CDN + "/development/old/a.png"));
        File file = new File();
        file.setFileName("old-name.png");
        file.setFileContent(CDN + "/legacy.png");
        service.attach(new UploadLifecycleService.References().files(List.of(file)));
        verify(repository, never()).save(any());
        verifyNoInteractions(storage);
    }

    @Test
    void pendingRequiresCurrentOwnerButAttachedCanBeShared() {
        authenticate("another-user", "ROLE_USER");
        assertThatThrownBy(() -> service.attach(new UploadLifecycleService.References().urls(CDN + "/" + KEY)))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(asset.getState()).isEqualTo(UploadAsset.State.PENDING);
        asset.attach();
        service.attach(new UploadLifecycleService.References().urls(CDN + "/" + KEY));
        verifyNoInteractions(storage);
    }

    @Test
    void rejectsOtherEnvironmentAndBucketEvenIfAttached() {
        assertThatThrownBy(() -> service.attach(new UploadLifecycleService.References()
                .urls(CDN + "/" + KEY.replace("development/", "production/"))))
                .isInstanceOf(ResponseStatusException.class);
        asset.attach();
        properties.setBucketName("another-bucket");
        assertThatThrownBy(() -> service.attach(new UploadLifecycleService.References().urls(CDN + "/" + KEY)))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(storage);
    }

    @ParameterizedTest
    @EnumSource(value = UploadAsset.State.class, names = {"PENDING", "DELETING", "DELETED"})
    void rejectsExpiredClaimedAndDeleted(UploadAsset.State state) {
        asset = asset(KEY, NOW);
        if (state != UploadAsset.State.PENDING) {
            asset.claim(NOW, NOW.plusSeconds(300), "claim");
        }
        if (state == UploadAsset.State.DELETED) {
            asset.deleted("claim");
        }
        when(repository.lockByObjectKey(KEY)).thenReturn(Optional.of(asset));
        assertThatThrownBy(() -> service.attach(new UploadLifecycleService.References().urls(CDN + "/" + KEY)))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(storage);
    }

    @Test
    void locksEveryReferenceOfOneSaveInGlobalOrder() {
        String secondKey = KEY.replace("0001.png", "0002.png");
        var second = asset(secondKey, NOW.plusSeconds(1000));
        when(repository.lockByObjectKey(secondKey)).thenReturn(Optional.of(second));
        when(storage.head("unit-test-bucket", secondKey)).thenReturn(new R2Storage.Metadata(100, "image/png"));
        File file = new File();
        file.setFileName(secondKey);
        file.setFileContent(CDN + "/" + secondKey);

        service.attach(new UploadLifecycleService.References()
                .files(List.of(file))
                .content("<img src='" + CDN + "/" + KEY + "'>"));

        var order = inOrder(repository);
        order.verify(repository).lockByObjectKey(KEY);
        order.verify(repository).lockByObjectKey(secondKey);
        assertThat(asset.getState()).isEqualTo(UploadAsset.State.ATTACHED);
        assertThat(second.getState()).isEqualTo(UploadAsset.State.ATTACHED);
    }

    @Test
    void caseInsensitiveDatabaseMatchCannotAttachANoncanonicalUrl() {
        String differentCase = KEY.replace("images", "IMAGES");
        when(repository.lockByObjectKey(differentCase)).thenReturn(Optional.of(asset));
        assertThatThrownBy(() -> service.attach(new UploadLifecycleService.References().urls(CDN + "/" + differentCase)))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(storage);
    }

    @Test
    void headMismatchOrFailureLeavesPending() {
        when(storage.head("unit-test-bucket", KEY)).thenReturn(new R2Storage.Metadata(101, "image/png"));
        assertThatThrownBy(() -> service.attach(new UploadLifecycleService.References().urls(CDN + "/" + KEY)))
                .isInstanceOf(ResponseStatusException.class);
        when(storage.head("unit-test-bucket", KEY)).thenReturn(new R2Storage.Metadata(100, "text/html"));
        assertThatThrownBy(() -> service.attach(new UploadLifecycleService.References().urls(CDN + "/" + KEY)))
                .isInstanceOf(ResponseStatusException.class);
        when(storage.head("unit-test-bucket", KEY)).thenThrow(new IllegalStateException("offline"));
        assertThatThrownBy(() -> service.attach(new UploadLifecycleService.References().urls(CDN + "/" + KEY)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(asset.getState()).isEqualTo(UploadAsset.State.PENDING);
    }
}
