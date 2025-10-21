package org.crochet.payload.response;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
public class FileResponse {
    private String fileName;
    private String fileContent;
    private Integer order;
    private Instant lastModifiedAt = Instant.now();

    public FileResponse(String fileName, String fileContent) {
        this(fileName, fileContent, 0);
    }

    public FileResponse(String fileName, String fileContent, Integer order) {
        this(fileName, fileContent, order, Instant.now());
    }

    public FileResponse(String fileName, String fileContent, Instant lastModifiedAt) {
        this(fileName, fileContent, 0, lastModifiedAt);
    }

    public FileResponse(String fileName, String fileContent, Integer order, Instant lastModifiedAt) {
        this.fileName = fileName;
        this.fileContent = fileContent;
        this.order = order;
        this.lastModifiedAt = lastModifiedAt;
    }
}
