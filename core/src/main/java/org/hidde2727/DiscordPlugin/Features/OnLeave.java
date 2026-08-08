package org.hidde2727.DiscordPlugin.Features;

import org.hidde2727.DiscordPlugin.Discord.EmbedInfo;
import org.hidde2727.DiscordPlugin.Feature;
import org.hidde2727.DiscordPlugin.Models.Player;
import org.hidde2727.DiscordPlugin.Storage.Config;
import org.hidde2727.DiscordPlugin.Storage.DataStorage;
import org.hidde2727.DiscordPlugin.Discord.Discord;

public class OnLeave extends Feature {
    private final Config.Events.OnLeave config;
    private final DataStorage.Maintenance maintenance;

    public OnLeave() {
        this.config = Config.getInstance().events.onLeave;
        this.maintenance = DataStorage.getInstance().maintenance;

        if(!config.enabled) return;

        if(!Discord.getInstance().checkChannel(config.channel, "onJoin")) {
            config.enabled = false;
        }
    }

    @Override
    public void onPlayerDisconnect(Player player) {
        if(!config.enabled) return;
        if(config.disableDuringMaintenance && maintenance.InMaintenance()) return;

        (new EmbedInfo())
            .setLanguage("events", "on-leave")
            .setVariables(player)
            .sendInChannel(config.channel);
    }
}
