package org.crochet.service.impl;

import org.crochet.enums.ResultCode;
import org.crochet.exception.BadRequestException;
import org.crochet.exception.ResourceNotFoundException;
import org.crochet.mapper.VideoPopupListMapper;
import org.crochet.model.File;
import org.crochet.model.VideoPopupClip;
import org.crochet.model.VideoPopupList;
import org.crochet.payload.VideoPopupClipPayload;
import org.crochet.payload.request.VideoPopupListRequest;
import org.crochet.payload.response.VideoPopupListResponse;
import org.crochet.repository.VideoPopupListRepo;
import org.crochet.service.UploadLifecycleService;
import org.crochet.service.VideoPopupListService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class VideoPopupListServiceImpl implements VideoPopupListService {
    private static final Pattern SAFE_INTERNAL_PATH = Pattern.compile("^/[^/\\\\].*");

    private final VideoPopupListRepo videoPopupListRepo;
    private final UploadLifecycleService uploads;

    public VideoPopupListServiceImpl(VideoPopupListRepo videoPopupListRepo, UploadLifecycleService uploads) {
        this.videoPopupListRepo = videoPopupListRepo;
        this.uploads = uploads;
    }

    @Transactional(readOnly = true)
    @Override
    public List<VideoPopupListResponse> getActive() {
        return VideoPopupListMapper.INSTANCE.toResponses(videoPopupListRepo.findByActiveTrueOrderByCreatedDateDesc());
    }

    @Transactional(readOnly = true)
    @Override
    public List<VideoPopupListResponse> getAll() {
        return VideoPopupListMapper.INSTANCE.toResponses(videoPopupListRepo.findAllByOrderByCreatedDateDesc());
    }

    @Transactional
    @Override
    public VideoPopupListResponse create(VideoPopupListRequest request) {
        List<VideoPopupClip> clips = validateAndNormalizeClips(request.getClips());
        attachUploads(clips);

        VideoPopupList entity = VideoPopupList.builder()
                .title(request.getTitle().trim())
                .active(request.isActive())
                .clips(new ArrayList<>(clips))
                .build();

        return VideoPopupListMapper.INSTANCE.toResponse(videoPopupListRepo.save(entity));
    }

    @Transactional
    @Override
    public VideoPopupListResponse update(String id, VideoPopupListRequest request) {
        VideoPopupList entity = findOrThrow(id);
        List<VideoPopupClip> clips = validateAndNormalizeClips(request.getClips());
        attachUploads(clips);

        entity.setTitle(request.getTitle().trim());
        entity.setActive(request.isActive());
        entity.setClips(new ArrayList<>(clips));

        return VideoPopupListMapper.INSTANCE.toResponse(videoPopupListRepo.save(entity));
    }

    @Transactional
    @Override
    public void delete(String id) {
        VideoPopupList entity = findOrThrow(id);
        videoPopupListRepo.delete(entity);
    }

    private VideoPopupList findOrThrow(String id) {
        return videoPopupListRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ResultCode.MSG_VIDEO_POPUP_LIST_NOT_FOUND.message(),
                        ResultCode.MSG_VIDEO_POPUP_LIST_NOT_FOUND.code()));
    }

    private List<VideoPopupClip> validateAndNormalizeClips(List<VideoPopupClipPayload> payloads) {
        List<VideoPopupClip> normalized = new ArrayList<>(payloads.size());
        for (VideoPopupClipPayload payload : payloads) {
            VideoPopupClip clip = VideoPopupListMapper.INSTANCE.toClip(payload);
            clip.setVideoUrl(clip.getVideoUrl().trim());
            clip.setVideoFileName(clip.getVideoFileName().trim());
            clip.setLabel(trimToNull(clip.getLabel()));

            String ctaUrl = trimToNull(clip.getCtaUrl());
            if (ctaUrl != null && !isSafeCtaUrl(ctaUrl)) {
                throw new BadRequestException(
                        ResultCode.MSG_VIDEO_POPUP_LIST_INVALID_CTA.message(),
                        ResultCode.MSG_VIDEO_POPUP_LIST_INVALID_CTA.code());
            }
            clip.setCtaUrl(ctaUrl);
            normalized.add(clip);
        }
        return normalized;
    }

    private void attachUploads(List<VideoPopupClip> clips) {
        var references = new UploadLifecycleService.References();
        references.files(clips.stream().map(clip -> {
            var file = new File();
            file.setFileName(clip.getVideoFileName());
            file.setFileContent(clip.getVideoUrl());
            return file;
        }).toList());
        uploads.attach(references);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean isSafeCtaUrl(String url) {
        if (SAFE_INTERNAL_PATH.matcher(url).matches()) {
            return true;
        }
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && host != null && !host.isBlank();
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
