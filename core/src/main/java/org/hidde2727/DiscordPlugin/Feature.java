package org.hidde2727.DiscordPlugin;

import org.hidde2727.DiscordPlugin.Discord.Discord;
import org.hidde2727.DiscordPlugin.Models.DiscordUser;
import org.hidde2727.DiscordPlugin.Models.Player;

/** Feature 'interface' */
public class Feature {

    public void onServerStart() {}
    public void onServerStop() {}
    public void onPlayerMessage(String onServer, Player player, String message) {}
    public boolean onPlayerPreLogin(Player player) { return true; }
    public void onPlayerConnect(Player player) {}
    public void onPlayerDisconnect(Player player) {}

    public void onPlayerAdd() {}
}
