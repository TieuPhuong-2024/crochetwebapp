package org.crochet.command.dispatcher;

import org.crochet.command.commands.Command;

public interface Dispatcher {
    <R, C extends Command<R>> R dispatch(C command);
}
