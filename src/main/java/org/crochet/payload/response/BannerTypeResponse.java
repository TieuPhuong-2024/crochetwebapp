package org.crochet.payload.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BannerTypeResponse {
    private String id;
    private String name;
    private Instant createdAt;
    private Instant lastModifiedAt;
}