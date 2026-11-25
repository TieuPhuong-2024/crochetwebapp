package org.crochet.service.impl;

import org.crochet.exception.BadRequestException;
import org.crochet.exception.ResourceNotFoundException;
import org.crochet.model.File;
import org.crochet.model.VideoPopupClip;
import org.crochet.model.VideoPopupList;
import org.crochet.payload.VideoPopupClipPayload;
import org.crochet.payload.request.VideoPopupListRequest;
import org.crochet.repository.VideoPopupListRepo;
import org.crochet.service.UploadLifecycleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideoPopupListServiceImplTest {

    @Mock
    private VideoPopupListRepo videoPopupListRepo;

    @Mock
    private UploadLifecycleService uploads;

    @InjectMocks
    private VideoPopupListServiceImpl service;

    @Test
    void create_persistsTrimmedTitleAndOrderedClipsAndAttachesUploads() {
        var first = clip("https://cdn/a.mp4", "key-a", "  First  ", " /patterns ");
        var second = clip("https://cdn/b.mp4", "key-b", "   ", "https://example.com/shop");
        var request = request("  Promo Ads  ", true, List.of(first, second));

        when(videoPopupListRepo.save(any(VideoPopupList.class))).thenAnswer(invocation -> {
            VideoPopupList entity = invocation.getArgument(0);
            entity.setId("list-1");
            return entity;
        });

        var saved = service.create(request);

        var entityCaptor = ArgumentCaptor.forClass(VideoPopupList.class);
        verify(videoPopupListRepo).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getTitle()).isEqualTo("Promo Ads");
        assertThat(entityCaptor.getValue().isActive()).isTrue();
        assertThat(entityCaptor.getValue().getClips())
                .extracting(VideoPopupClip::getVideoFileName)
                .containsExactly("key-a", "key-b");
        assertThat(entityCaptor.getValue().getClips().get(0).getLabel()).isEqualTo("First");
        assertThat(entityCaptor.getValue().getClips().get(0).getCtaUrl()).isEqualTo("/patterns");
        assertThat(entityCaptor.getValue().getClips().get(1).getLabel()).isNull();

        var references = ArgumentCaptor.forClass(UploadLifecycleService.References.class);
        verify(uploads).attach(references.capture());
        assertThat(references.getValue().files()).extracting(File::getFileName).containsExactly("key-a", "key-b");
        assertThat(references.getValue().files()).extracting(File::getFileContent)
                .containsExactly("https://cdn/a.mp4", "https://cdn/b.mp4");

        assertThat(saved.getId()).isEqualTo("list-1");
        assertThat(saved.getClips()).hasSize(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"javascript:alert(1)", "//evil.com", "/\\evil.com", "ftp://example.com/a"})
    void create_rejectsUnsafeCtaUrl(String unsafeUrl) {
        var request = request("Ads", true, List.of(clip("https://cdn/a.mp4", "key-a", "CTA", unsafeUrl)));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(uploads, videoPopupListRepo);
    }

    @Test
    void update_updatesEntityAndAttachesUploads() {
        var existing = VideoPopupList.builder()
                .id("list-1")
                .title("Old")
                .active(false)
                .clips(List.of(new VideoPopupClip("https://cdn/old.mp4", "key-old", null, null)))
                .build();
        when(videoPopupListRepo.findById("list-1")).thenReturn(Optional.of(existing));
        when(videoPopupListRepo.save(any(VideoPopupList.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var request = request("  Updated  ", true, List.of(clip("https://cdn/new.mp4", "key-new", "Watch", "/shop")));
        var updated = service.update("list-1", request);

        assertThat(updated.getTitle()).isEqualTo("Updated");
        assertThat(updated.isActive()).isTrue();
        assertThat(updated.getClips()).extracting(VideoPopupClipPayload::getVideoFileName).containsExactly("key-new");
        verify(uploads).attach(any(UploadLifecycleService.References.class));
        verify(videoPopupListRepo).save(existing);
    }

    @Test
    void update_throwsWhenNotFound() {
        when(videoPopupListRepo.findById("missing")).thenReturn(Optional.empty());
        var request = request("Ads", true, List.of(clip("https://cdn/a.mp4", "key-a", null, null)));

        assertThatThrownBy(() -> service.update("missing", request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_deletesExistingEntity() {
        var existing = VideoPopupList.builder().id("list-1").title("Ads").active(true).build();
        when(videoPopupListRepo.findById("list-1")).thenReturn(Optional.of(existing));

        service.delete("list-1");

        verify(videoPopupListRepo).delete(existing);
    }

    @Test
    void delete_throwsWhenNotFound() {
        when(videoPopupListRepo.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getActive_returnsRepoOrder() {
        var entity = VideoPopupList.builder()
                .title("One")
                .active(true)
                .clips(List.of(
                        new VideoPopupClip("https://cdn/a.mp4", "key-a", null, null),
                        new VideoPopupClip("https://cdn/b.mp4", "key-b", null, null)))
                .build();
        when(videoPopupListRepo.findByActiveTrueOrderByCreatedDateDesc()).thenReturn(List.of(entity));

        var result = service.getActive();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getClips())
                .extracting(VideoPopupClipPayload::getVideoUrl)
                .containsExactly("https://cdn/a.mp4", "https://cdn/b.mp4");
    }

    private static VideoPopupListRequest request(String title, boolean active, List<VideoPopupClipPayload> clips) {
        var request = new VideoPopupListRequest();
        request.setTitle(title);
        request.setActive(active);
        request.setClips(clips);
        return request;
    }

    private static VideoPopupClipPayload clip(String url, String key, String label, String ctaUrl) {
        var clip = new VideoPopupClipPayload();
        clip.setVideoUrl(url);
        clip.setVideoFileName(key);
        clip.setLabel(label);
        clip.setCtaUrl(ctaUrl);
        return clip;
    }
}
