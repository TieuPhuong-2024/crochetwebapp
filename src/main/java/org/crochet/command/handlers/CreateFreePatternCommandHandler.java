package org.crochet.command.handlers;

import lombok.RequiredArgsConstructor;
import org.crochet.command.commands.CreateFreePatternCommand;
import org.crochet.service.FreePatternService;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component("CreateFreePatternCommandHandler")
public class CreateFreePatternCommandHandler implements CommandHandler<String, CreateFreePatternCommand> {

    private final FreePatternService freePatternService;

    @Override
    public String handle(CreateFreePatternCommand command) {
        freePatternService.createOrUpdate(command);
        return "Create free pattern success";
    }
}
