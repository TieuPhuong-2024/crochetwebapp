package org.crochet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.crochet.enums.ResultCode;
import org.crochet.payload.request.BannerTypeRequest;
import org.crochet.payload.response.BannerTypeResponse;
import org.crochet.payload.response.ResponseData;
import org.crochet.service.BannerTypeService;
import org.crochet.util.ResponseUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/banner-types")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "BearerAuth")
public class BannerTypeController {
    final BannerTypeService bannerTypeService;

    public BannerTypeController(BannerTypeService bannerTypeService) {
        this.bannerTypeService = bannerTypeService;
    }

    @Operation(summary = "Create or update banner type")
    @ApiResponse(responseCode = "201", description = "Banner type created or updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BannerTypeResponse.class)))
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ResponseData<BannerTypeResponse> createOrUpdate(@RequestBody BannerTypeRequest request) {
        var res = bannerTypeService.createOrUpdate(request);
        return ResponseUtil.success(res);
    }

    @Operation(summary = "Delete banner type by ID")
    @ApiResponse(responseCode = "200", description = "Banner type deleted successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class)))
    @ResponseStatus(HttpStatus.OK)
    @DeleteMapping("/{id}")
    public ResponseData<String> delete(@PathVariable("id") String id) {
        bannerTypeService.delete(id);
        return ResponseUtil.success(ResultCode.MSG_DELETE_SUCCESS.message());
    }

    @Operation(summary = "Get all banner types")
    @ApiResponse(responseCode = "200", description = "Banner types retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = List.class)))
    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    public ResponseData<List<BannerTypeResponse>> getAll() {
        var res = bannerTypeService.getAll();
        return ResponseUtil.success(res);
    }

    @Operation(summary = "Get banner type by ID")
    @ApiResponse(responseCode = "200", description = "Banner type retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BannerTypeResponse.class)))
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{id}")
    public ResponseData<BannerTypeResponse> getById(@PathVariable("id") String id) {
        var res = bannerTypeService.getById(id);
        return ResponseUtil.success(res);
    }
}
