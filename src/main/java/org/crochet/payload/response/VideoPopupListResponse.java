package org.crochet.payload.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import org.crochet.constant.AppConstant;
import org.crochet.payload.VideoPopupClipPayload;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class VideoPopupListResponse {
    private String id;
    private String title;
    private String ctaUrl;
    private String ctaLabel;
    private boolean active;
    private List<VideoPopupClipPayload> clips;

    @JsonFormat(pattern = AppConstant.DATE_PATTERN)
    private LocalDateTime createdDate;

    @JsonFormat(pattern = AppConstant.DATE_PATTERN)
    private LocalDateTime lastModifiedDate;
}
