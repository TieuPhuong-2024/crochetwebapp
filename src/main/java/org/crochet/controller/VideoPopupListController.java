package org.crochet.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.crochet.payload.request.VideoPopupListRequest;
import org.crochet.payload.response.ResponseData;
import org.crochet.payload.response.VideoPopupListResponse;
import org.crochet.service.VideoPopupListService;
import org.crochet.util.ResponseUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/video-popup-lists")
public class VideoPopupListController {
    private final VideoPopupListService videoPopupListService;

    public VideoPopupListController(VideoPopupListService videoPopupListService) {
        this.videoPopupListService = videoPopupListService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ResponseData<List<VideoPopupListResponse>> batchInsertOrUpdate(
            @Valid @RequestBody List<VideoPopupListRequest> requests) {
        var res = videoPopupListService.batchInsertOrUpdate(requests);
        return ResponseUtil.success(res, HttpStatus.CREATED);
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    public ResponseData<List<VideoPopupListResponse>> getActive() {
        return ResponseUtil.success(videoPopupListService.getActive());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/manage")
    public ResponseData<List<VideoPopupListResponse>> getAll() {
        return ResponseUtil.success(videoPopupListService.getAll());
    }
}
