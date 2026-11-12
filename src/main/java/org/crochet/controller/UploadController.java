package org.crochet.controller;

import jakarta.validation.Valid;
import org.crochet.payload.request.PresignedUploadRequest;
import org.crochet.payload.response.PresignedUploadResponse;
import org.crochet.payload.response.ResponseData;
import org.crochet.service.UploadReservationService;
import org.crochet.util.ResponseUtil;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/uploads")
@PreAuthorize("isAuthenticated()")
public class UploadController {
    private final UploadReservationService reservations;

    public UploadController(UploadReservationService reservations) {
        this.reservations = reservations;
    }

    @PostMapping("/presigned")
    public ResponseData<PresignedUploadResponse> presigned(@Valid @RequestBody PresignedUploadRequest request) {
        return ResponseUtil.success(reservations.reserve(request));
    }
}
