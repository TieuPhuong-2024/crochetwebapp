package org.crochet.payload.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PresignedUploadRequest(
        @NotBlank @Size(max = 40) String module,
        @NotBlank @Size(max = 40) String category,
        @NotBlank @Size(max = 255) String fileName,
        @NotBlank @Size(max = 100) String contentType,
        @NotNull @Positive @Max(104857600) Long size) {
}
