package org.hidde2727.DiscordPlugin.Features;

import org.hidde2727.DiscordPlugin.Discord.EmbedInfo;
import org.hidde2727.DiscordPlugin.Feature;
import org.hidde2727.DiscordPlugin.Models.Player;
import org.hidde2727.DiscordPlugin.Storage.Config;
import org.hidde2727.DiscordPlugin.Storage.DataStorage;
import org.hidde2727.DiscordPlugin.Discord.Discord;

public class OnJoin extends Feature {
    private final Config.Events.OnJoin config;
    private final DataStorage.Maintenance maintenance;

    public OnJoin() {
        this.config = Config.getInstance().events.onJoin;
        this.maintenance = DataStorage.getInstance().maintenance;

        if(!config.enabled) return;

        if(!Discord.getInstance().checkChannel(config.channel, "onJoin")) {
            config.enabled = false;
        }
    }

    @Override
    public void onPlayerConnect(Player player) {
        if(!config.enabled) return;
        if(config.disableDuringMaintenance && maintenance.InMaintenance()) return;

        (new EmbedInfo())
            .setLanguage("events", "on-join")
            .setVariables(player)
            .sendInChannel(config.channel);
    }
}
