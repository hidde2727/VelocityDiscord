package org.hidde2727.DiscordPlugin.Implementation;

import java.nio.file.Path;

public interface Implementation {
    public void debug(String message);
    public void info(String message);
    public void warn(String message);
    public void error(String message);
    public Path GetDataDirectory();
    public boolean IsOnlineMode();
    public void SendMessage(String serverID, String message);
}
