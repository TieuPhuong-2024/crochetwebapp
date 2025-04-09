package org.crochet.command.dispatcher;

import org.crochet.command.commands.Command;
import org.crochet.command.handlers.CommandHandler;
import org.crochet.command.factory.CommandHandlerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service
public class SpringDispatcher implements Dispatcher {

    private final ApplicationContext applicationContext;

    public SpringDispatcher(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public <R, C extends Command<R>> R dispatch(C command) {
        CommandHandler<R, C> handler = (CommandHandler<R, C>) applicationContext.getBean(CommandHandlerFactory.getHandlerBeanName(command.getClass()));
        return handler.handle(command);
    }
}
