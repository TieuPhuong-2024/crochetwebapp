package org.crochet.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.crochet.converter.VideoPopupClipListConverter;
import org.crochet.payload.VideoPopupClipPayload;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "video_popup_list")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class VideoPopupList extends BaseEntity {
    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "cta_url", length = 500)
    private String ctaUrl;

    @Column(name = "cta_label")
    private String ctaLabel;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Convert(converter = VideoPopupClipListConverter.class)
    @Column(name = "clips", columnDefinition = "text", nullable = false)
    private List<VideoPopupClipPayload> clips = new ArrayList<>();
}
