package org.crochet.service.impl;

import org.crochet.enums.ResultCode;
import org.crochet.exception.BadRequestException;
import org.crochet.exception.ResourceNotFoundException;
import org.crochet.mapper.VideoPopupListMapper;
import org.crochet.model.VideoPopupList;
import org.crochet.model.File;
import org.crochet.service.UploadLifecycleService;
import org.crochet.payload.VideoPopupClipPayload;
import org.crochet.payload.request.VideoPopupListRequest;
import org.crochet.payload.response.VideoPopupListResponse;
import org.crochet.repository.VideoPopupListRepo;
import org.crochet.service.VideoPopupListService;
import org.crochet.util.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class VideoPopupListServiceImpl implements VideoPopupListService {
    private final VideoPopupListRepo videoPopupListRepo;
    private final UploadLifecycleService uploads;

    public VideoPopupListServiceImpl(VideoPopupListRepo videoPopupListRepo, UploadLifecycleService uploads) {
        this.videoPopupListRepo = videoPopupListRepo;
        this.uploads = uploads;
    }

    @Transactional
    @Override
    public List<VideoPopupListResponse> batchInsertOrUpdate(List<VideoPopupListRequest> requests) {
        List<VideoPopupList> lists = new ArrayList<>();
        List<VideoPopupList> existing = videoPopupListRepo.findAll();
        var references = new UploadLifecycleService.References();

        for (VideoPopupListRequest request : requests) {
            validateClips(request.getClips());
            VideoPopupList entity;
            if (!ObjectUtils.hasText(request.getId())) {
                entity = VideoPopupListMapper.INSTANCE.toEntity(request);
            } else {
                entity = videoPopupListRepo.findById(request.getId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                ResultCode.MSG_VIDEO_POPUP_LIST_NOT_FOUND.message(),
                                ResultCode.MSG_VIDEO_POPUP_LIST_NOT_FOUND.code()));
                VideoPopupListMapper.INSTANCE.partialUpdate(request, entity);
                entity.setClips(request.getClips());
            }
            references.files(entity.getClips().stream().map(clip -> {
                var file = new File();
                file.setFileName(clip.getVideoFileName());
                file.setFileContent(clip.getVideoUrl());
                return file;
            }).toList());
            lists.add(entity);
        }
        uploads.attach(references);

        existing.removeAll(lists);
        videoPopupListRepo.deleteAll(existing);
        return VideoPopupListMapper.INSTANCE.toResponses(videoPopupListRepo.saveAll(lists));
    }

    @Transactional(readOnly = true)
    @Override
    public List<VideoPopupListResponse> getActive() {
        return VideoPopupListMapper.INSTANCE.toResponses(videoPopupListRepo.findByActiveTrueOrderByCreatedDateDesc());
    }

    @Transactional(readOnly = true)
    @Override
    public List<VideoPopupListResponse> getAll() {
        return VideoPopupListMapper.INSTANCE.toResponses(videoPopupListRepo.findAll());
    }

    private void validateClips(List<VideoPopupClipPayload> clips) {
        if (clips == null || clips.isEmpty()) {
            throw new BadRequestException(
                    ResultCode.MSG_VIDEO_POPUP_LIST_CLIPS_REQUIRED.message(),
                    ResultCode.MSG_VIDEO_POPUP_LIST_CLIPS_REQUIRED.code());
        }
        for (VideoPopupClipPayload clip : clips) {
            if (clip == null || !ObjectUtils.hasText(clip.getVideoUrl()) || !ObjectUtils.hasText(clip.getVideoFileName())) {
                throw new BadRequestException(
                        ResultCode.MSG_VIDEO_POPUP_LIST_CLIPS_REQUIRED.message(),
                        ResultCode.MSG_VIDEO_POPUP_LIST_CLIPS_REQUIRED.code());
            }
        }
    }
}
