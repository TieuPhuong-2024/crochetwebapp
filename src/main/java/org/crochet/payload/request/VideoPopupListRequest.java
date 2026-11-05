package org.crochet.payload.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.crochet.payload.VideoPopupClipPayload;

import java.util.List;

@Getter
@Setter
public class VideoPopupListRequest {
    private String id;

    @NotBlank
    private String title;

    private boolean active;

    @NotEmpty
    @Valid
    private List<VideoPopupClipPayload> clips;
}
