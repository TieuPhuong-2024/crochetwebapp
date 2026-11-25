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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ResponseData<VideoPopupListResponse> create(@Valid @RequestBody VideoPopupListRequest request) {
        return ResponseUtil.success(videoPopupListService.create(request), HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/{id}")
    public ResponseData<VideoPopupListResponse> update(
            @PathVariable String id,
            @Valid @RequestBody VideoPopupListRequest request) {
        return ResponseUtil.success(videoPopupListService.update(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @ResponseStatus(HttpStatus.OK)
    @DeleteMapping("/{id}")
    public ResponseData<Void> delete(@PathVariable String id) {
        videoPopupListService.delete(id);
        return ResponseUtil.success();
    }
}
