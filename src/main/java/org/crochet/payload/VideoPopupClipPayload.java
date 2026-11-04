package org.crochet.payload;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VideoPopupClipPayload {
    @NotBlank
    private String videoUrl;
    @NotBlank
    private String videoFileName;
    private String label;
}
