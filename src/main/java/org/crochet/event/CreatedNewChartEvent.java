package org.crochet.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreatedNewChartEvent {
    String creatorId;
    String chartId;
    String chartName;
}
