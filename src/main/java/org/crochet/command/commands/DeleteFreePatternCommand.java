package org.crochet.command.commands;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeleteFreePatternCommand implements Command<Boolean> {
    private String freePatternId;
}
