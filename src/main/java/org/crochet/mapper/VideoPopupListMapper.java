package org.crochet.mapper;

import org.crochet.model.VideoPopupClip;
import org.crochet.model.VideoPopupList;
import org.crochet.payload.VideoPopupClipPayload;
import org.crochet.payload.response.VideoPopupListResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface VideoPopupListMapper extends AbstractMapper<VideoPopupList, VideoPopupListResponse> {
    VideoPopupListMapper INSTANCE = Mappers.getMapper(VideoPopupListMapper.class);

    VideoPopupClip toClip(VideoPopupClipPayload payload);

    VideoPopupClipPayload toClipPayload(VideoPopupClip clip);

    List<VideoPopupClip> toClips(List<VideoPopupClipPayload> payloads);

    @Override
    VideoPopupListResponse toResponse(VideoPopupList entity);

    List<VideoPopupListResponse> toResponses(List<VideoPopupList> entities);
}
