package org.crochet.controller;

import com.turkraft.springfilter.boot.Filter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.crochet.constant.AppConstant;
import org.crochet.enums.ResultCode;
import org.crochet.model.FreePattern;
import org.crochet.model.User;
import org.crochet.payload.request.FreePatternRequest;
import org.crochet.payload.response.FreePatternResponse;
import org.crochet.payload.response.PaginationResponse;
import org.crochet.payload.response.ResponseData;
import org.crochet.security.CurrentUser;
import org.crochet.service.FreePatternService;
import org.crochet.util.ResponseUtil;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import org.crochet.service.PdfService;

@RestController
@RequestMapping("/api/v1/free-pattern")
public class FreePatternController {
    private final FreePatternService freePatternService;
    private final PdfService pdfService;

    public FreePatternController(FreePatternService freePatternService, PdfService pdfService) {
        this.freePatternService = freePatternService;
        this.pdfService = pdfService;
    }

    @Operation(summary = "Create a free pattern")
    @ApiResponse(responseCode = "201", description = "Free Pattern created successfully",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = FreePatternResponse.class)))
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseData<String> createPattern(@RequestBody FreePatternRequest request) {
        freePatternService.createOrUpdate(request);
        return ResponseUtil.success(ResultCode.MSG_CREATE_OR_UPDATE_SUCCESS.message());
    }

    @Operation(summary = "Get pattern details by ID")
    @ApiResponse(responseCode = "200", description = "Free Pattern details retrieved successfully",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = FreePatternResponse.class)))
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{id}")
    public ResponseData<FreePatternResponse> getDetail(
            @Parameter(description = "ID of the pattern to retrieve")
            @PathVariable("id") String id) {
        var response = freePatternService.getDetail(id);
        return ResponseUtil.success(response);
    }

    @Operation(summary = "Delete a free pattern")
    @ApiResponse(responseCode = "200", description = "Free pattern deleted successfully",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = String.class)))
    @ResponseStatus(HttpStatus.OK)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseData<String> delete(
            @Parameter(description = "ID of the pattern to delete")
            @PathVariable("id") String id) {
        freePatternService.delete(id);
        return ResponseUtil.success(ResultCode.MSG_DELETE_SUCCESS.message());
    }

    @Operation(summary = "Delete free patterns by ids")
    @ApiResponse(responseCode = "200", description = "Free patterns deleted successfully",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ResponseData.class)))
    @ResponseStatus(HttpStatus.OK)
    @DeleteMapping("/bulk")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseData<String> deleteMultiple(
            @Parameter(description = "List of pattern ids to delete")
            @RequestBody List<String> ids) {
        freePatternService.deleteAllById(ids);
        return ResponseUtil.success(ResultCode.MSG_DELETE_SUCCESS.message());
    }

    @Operation(summary = "Get paginated list of patterns")
    @ApiResponse(responseCode = "200", description = "List of free patterns",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PaginationResponse.class)))
    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    public ResponseData<PaginationResponse<FreePatternResponse>> getAllFreePatterns(
            @Parameter(description = "Page number (default: 0)")
            @RequestParam(value = "pageNo", defaultValue = AppConstant.DEFAULT_PAGE_NUMBER,
                    required = false) int pageNo,
            @Parameter(description = "Page size (default: 48)")
            @RequestParam(value = "pageSize", defaultValue = AppConstant.DEFAULT_PAGE_SIZE,
                    required = false) int pageSize,
            @Parameter(description = "Sort by field (default: createdDate)")
            @RequestParam(value = "sortBy", defaultValue = AppConstant.DEFAULT_SORT_BY, required = false) String sortBy,
            @Parameter(description = "Sort direction (default: DESC)")
            @RequestParam(value = "sortDir", defaultValue = AppConstant.DEFAULT_SORT_DIRECTION,
                    required = false) String sortDir,
            @RequestParam(value = "categoryId", required = false) String categoryId,
            @Filter Specification<FreePattern> spec) {
        var response = freePatternService.getAllFreePatterns(pageNo, pageSize, sortBy, sortDir, categoryId, spec);
        return ResponseUtil.success(response);
    }

    @Operation(summary = "Get free pattern ids")
    @ApiResponse(responseCode = "200", description = "List of free pattern ids",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = List.class)))
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/ids")
    public ResponseData<List<String>> getFreePatternIds(@RequestParam("pageNo") int pageNo,
                                                        @RequestParam("pageSize") int pageSize) {
        var res = freePatternService.getFreePatternIds(pageNo, pageSize);
        return ResponseUtil.success(res);
    }

    @Operation(summary = "Check if a free pattern exists in collection")
    @ApiResponse(responseCode = "200", description = "Free pattern exists in collection",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Boolean.class)))
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{id}/exist")
    public ResponseData<Boolean> existsInCollection(@PathVariable("id") String id, @CurrentUser User user) {
        var res = freePatternService.existByFreePatternAndUser(id, user);
        return ResponseUtil.success(res);
    }

    @Operation(summary = "Check if a free pattern is liked by current user")
    @ApiResponse(responseCode = "200", description = "Like status check result",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Boolean.class)))
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{id}/is-liked")
    public ResponseData<Boolean> isLiked(@PathVariable("id") String id, @CurrentUser User user) {
        var res = freePatternService.existLikeByFreePatternAndUser(id, user);
        return ResponseUtil.success(res);
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FreePatternController.class);

    @Operation(summary = "Export free patterns as a ZIP of PDFs")
    @ApiResponse(responseCode = "200", description = "ZIP file containing PDF patterns",
            content = @Content(mediaType = "application/zip"))
    @PostMapping("/export")
    @PreAuthorize("hasAnyRole('PREMIUM_USER', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    public void exportPatternsToZip(
            @RequestBody List<String> ids,
            @CurrentUser User user,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"my-crochet-charts.zip\"");

        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(response.getOutputStream())) {
            for (String id : ids) {
                try {
                    FreePattern pattern = freePatternService.findById(id);
                    // Check if current user owns the pattern, or if user is ADMIN
                    if (user.getRole() != org.crochet.enums.RoleType.ADMIN && 
                        (pattern.getCreatedBy() == null || !pattern.getCreatedBy().equals(user.getId()))) {
                        // Skip if user does not own this pattern
                        continue;
                    }
                    
                    byte[] pdfBytes = pdfService.generatePatternPdf(pattern);
                    
                    // Create zip entry
                    String safeFileName = pattern.getName().replaceAll("[\\\\/:*?\"<>|]", "_") + ".pdf";
                    java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(safeFileName);
                    zos.putNextEntry(entry);
                    zos.write(pdfBytes);
                    zos.closeEntry();
                } catch (Exception e) {
                    log.error("Failed to export pattern id: " + id, e);
                }
            }
            zos.finish();
        }
    }

    @Operation(summary = "Export a single free pattern as PDF")
    @ApiResponse(responseCode = "200", description = "PDF pattern file",
            content = @Content(mediaType = "application/pdf"))
    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('PREMIUM_USER', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    public void exportSinglePatternToPdf(
            @PathVariable("id") String id,
            @CurrentUser User user,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        
        FreePattern pattern = freePatternService.findById(id);
        if (user.getRole() != org.crochet.enums.RoleType.ADMIN && 
            (pattern.getCreatedBy() == null || !pattern.getCreatedBy().equals(user.getId()))) {
            response.sendError(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return;
        }

        byte[] pdfBytes = pdfService.generatePatternPdf(pattern);
        response.setContentType("application/pdf");
        String safeFileName = pattern.getName().replaceAll("[\\\\/:*?\"<>|]", "_") + ".pdf";
        response.setHeader("Content-Disposition", "attachment; filename=\"" + safeFileName + "\"");
        response.setContentLength(pdfBytes.length);
        response.getOutputStream().write(pdfBytes);
        response.getOutputStream().flush();
    }
}
