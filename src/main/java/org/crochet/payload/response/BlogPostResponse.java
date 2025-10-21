package org.crochet.payload.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BlogPostResponse {
    private String id;
    private String title;
    private String content;
    @JsonProperty("is_home")
    private Boolean isHome;
    private List<FileResponse> files;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Instant createdAt;
    private String fileContent;
    private String createdBy;
    private String userId;
    private String username;
    private String userAvatar;
    private Long commentCount;

    public BlogPostResponse(String id,
                             String title,
                             String content,
                             String fileContent,
                             Instant createdAt,
                             String createdBy) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.fileContent = fileContent;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    public BlogPostResponse(String id,
                             String title,
                             String content,
                             String fileContent,
                             Instant createdAt,
                             String createdBy,
                             String userId,
                             String username,
                             String userAvatar) {
        this(id, title, content, fileContent, createdAt, createdBy);
        this.userId = userId;
        this.username = username;
        this.userAvatar = userAvatar;
    }
}
