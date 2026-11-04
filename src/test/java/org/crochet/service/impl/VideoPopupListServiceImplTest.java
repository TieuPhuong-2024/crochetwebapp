package org.crochet.service.impl;

import org.crochet.exception.BadRequestException;
import org.crochet.model.VideoPopupList;
import org.crochet.payload.VideoPopupClipPayload;
import org.crochet.payload.request.VideoPopupListRequest;
import org.crochet.repository.VideoPopupListRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideoPopupListServiceImplTest {

    @Mock
    private VideoPopupListRepo videoPopupListRepo;

    @InjectMocks
    private VideoPopupListServiceImpl service;

    @Test
    void batchInsert_persistsClipOrder() {
        var first = clip("https://cdn/a.mp4", "key-a");
        var second = clip("https://cdn/b.mp4", "key-b");
        var request = new VideoPopupListRequest();
        request.setTitle("Ads");
        request.setActive(true);
        request.setClips(List.of(first, second));

        when(videoPopupListRepo.findAll()).thenReturn(new ArrayList<>());
        when(videoPopupListRepo.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        var saved = service.batchInsertOrUpdate(List.of(request));

        ArgumentCaptor<List<VideoPopupList>> captor = ArgumentCaptor.forClass(List.class);
        verify(videoPopupListRepo).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getClips())
                .extracting(VideoPopupClipPayload::getVideoFileName)
                .containsExactly("key-a", "key-b");
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getClips()).hasSize(2);
    }

    @Test
    void batchInsert_rejectsEmptyClips() {
        var request = new VideoPopupListRequest();
        request.setTitle("Empty");
        request.setClips(List.of());
        when(videoPopupListRepo.findAll()).thenReturn(new ArrayList<>());

        assertThatThrownBy(() -> service.batchInsertOrUpdate(List.of(request)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void getActive_returnsRepoOrder() {
        var entity = VideoPopupList.builder()
                .title("One")
                .active(true)
                .clips(List.of(clip("https://cdn/a.mp4", "key-a"), clip("https://cdn/b.mp4", "key-b")))
                .build();
        when(videoPopupListRepo.findByActiveTrueOrderByCreatedDateDesc()).thenReturn(List.of(entity));

        var result = service.getActive();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getClips())
                .extracting(VideoPopupClipPayload::getVideoUrl)
                .containsExactly("https://cdn/a.mp4", "https://cdn/b.mp4");
    }

    private static VideoPopupClipPayload clip(String url, String key) {
        var clip = new VideoPopupClipPayload();
        clip.setVideoUrl(url);
        clip.setVideoFileName(key);
        return clip;
    }
}
