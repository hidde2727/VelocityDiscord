package org.hidde2727.DiscordPlugin;

import org.hidde2727.DiscordPlugin.Implementation.ActiveImplementation;

public class Logs {

    public static void debug(String message) {
        ActiveImplementation.active.debug(message);
    }
    public static void info(String message) {
        ActiveImplementation.active.info(message);
    }
    public static void warn(String message) {
        ActiveImplementation.active.warn(message);
    }
    public static void error(String message) {
        ActiveImplementation.active.error(message);
    }

}
