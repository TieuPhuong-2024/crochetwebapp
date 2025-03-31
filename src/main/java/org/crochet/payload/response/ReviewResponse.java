package org.crochet.payload.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReviewResponse {
    private String id;
    private String freePatternId;
    private String freePatternName;
    private String userId;
    private String username;
    private String userAvatar;
    private Integer rating;
    private String title;
    private String content;
    private LocalDateTime createdDate;
} 