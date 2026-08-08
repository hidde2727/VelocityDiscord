package org.hidde2727.DiscordPlugin;

import java.nio.file.Path;

public interface Implementation {
    public void debug(String message);
    public void info(String message);
    public void warn(String message);
    public void error(String message);
    public Path getDataDirectory();
    public boolean isOnlineMode();
    public void sendMessage(String serverID, String message);
}
