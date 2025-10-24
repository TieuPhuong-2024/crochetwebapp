package org.crochet.payload.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String createdAt;
    private String fileContent;
    private String createdBy;
    private String userId;
    private String username;
    private String userAvatar;
    private Long commentCount;
}
