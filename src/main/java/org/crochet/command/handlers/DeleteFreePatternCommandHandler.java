package org.crochet.command.handlers;

import lombok.RequiredArgsConstructor;
import org.crochet.command.commands.DeleteFreePatternCommand;
import org.crochet.service.FreePatternService;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component("DeleteFreePatternCommandHandler")
public class DeleteFreePatternCommandHandler implements CommandHandler<Boolean, DeleteFreePatternCommand> {

    private final FreePatternService freePatternService;

    @Override
    public Boolean handle(DeleteFreePatternCommand command) {
        freePatternService.delete(command.getFreePatternId());
        return true;
    }
}
