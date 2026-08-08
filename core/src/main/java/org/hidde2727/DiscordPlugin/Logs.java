package org.hidde2727.DiscordPlugin;

public class Logs {

    static Implementation useForLogging;

    public static void debug(String message) {
        useForLogging.debug(message);
    }
    public static void info(String message) {
        useForLogging.info(message);
    }
    public static void warn(String message) {
        useForLogging.warn(message);
    }
    public static void error(String message) {
        useForLogging.error(message);
    }

}
