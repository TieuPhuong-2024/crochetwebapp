package org.crochet.mapper;

import org.crochet.model.VideoPopupList;
import org.crochet.payload.request.VideoPopupListRequest;
import org.crochet.payload.response.VideoPopupListResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface VideoPopupListMapper extends AbstractMapper<VideoPopupList, VideoPopupListResponse>,
        PartialUpdate<VideoPopupList, VideoPopupListRequest> {
    VideoPopupListMapper INSTANCE = Mappers.getMapper(VideoPopupListMapper.class);

    @Mapping(target = "id", ignore = true)
    VideoPopupList toEntity(VideoPopupListRequest request);
}
