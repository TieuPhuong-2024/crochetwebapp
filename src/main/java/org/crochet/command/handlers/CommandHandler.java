package org.crochet.command.handlers;

import org.crochet.command.commands.Command;

public interface CommandHandler<R, C extends Command<R>> {
    R handle(C command);
}
