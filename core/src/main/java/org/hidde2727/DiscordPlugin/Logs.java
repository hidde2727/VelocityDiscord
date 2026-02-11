package org.hidde2727.DiscordPlugin;

public class Logs {

    static DiscordPlugin useForLogging;

    public static void debug(String message) {
        useForLogging.implementation.debug(message);
    }
    public static void info(String message) {
        useForLogging.implementation.info(message);
    }
    public static void warn(String message) {
        useForLogging.implementation.warn(message);
    }
    public static void error(String message) {
        useForLogging.implementation.error(message);
    }

}
