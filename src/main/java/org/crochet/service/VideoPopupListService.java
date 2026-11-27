package org.crochet.service;

import org.crochet.payload.request.VideoPopupListRequest;
import org.crochet.payload.response.VideoPopupListResponse;

import java.util.List;

public interface VideoPopupListService {
    List<VideoPopupListResponse> getActive();

    List<VideoPopupListResponse> getAll();

    VideoPopupListResponse create(VideoPopupListRequest request);

    VideoPopupListResponse update(String id, VideoPopupListRequest request);

    void delete(String id);
}
