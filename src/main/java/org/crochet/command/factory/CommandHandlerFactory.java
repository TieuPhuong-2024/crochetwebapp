package org.crochet.command.factory;

public class CommandHandlerFactory {
    public static String getHandlerBeanName(Class<?> commandClass) {
        return commandClass.getSimpleName() + "Handler";
    }
}
