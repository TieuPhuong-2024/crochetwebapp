package org.crochet.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoPopupClip {
    private String videoUrl;
    private String videoFileName;
    private String label;
    private String ctaUrl;
}
