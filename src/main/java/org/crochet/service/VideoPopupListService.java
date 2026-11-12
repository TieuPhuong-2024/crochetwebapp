package org.crochet.service;

import org.crochet.payload.request.VideoPopupListRequest;
import org.crochet.payload.response.VideoPopupListResponse;

import java.util.List;

public interface VideoPopupListService {
    List<VideoPopupListResponse> batchInsertOrUpdate(List<VideoPopupListRequest> requests);

    List<VideoPopupListResponse> getActive();

    List<VideoPopupListResponse> getAll();
}
