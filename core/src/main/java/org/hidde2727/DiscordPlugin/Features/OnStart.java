package org.hidde2727.DiscordPlugin.Features;

import org.hidde2727.DiscordPlugin.Discord.EmbedInfo;
import org.hidde2727.DiscordPlugin.Feature;
import org.hidde2727.DiscordPlugin.Storage.Config;
import org.hidde2727.DiscordPlugin.Storage.DataStorage;
import org.hidde2727.DiscordPlugin.Discord.Discord;

public class OnStart extends Feature {
    private final Config.Events.OnStart config;
    private final DataStorage.Maintenance maintenance;

    public OnStart() {
        this.config = Config.getInstance().events.onStart;
        this.maintenance = DataStorage.getInstance().maintenance;

        if(!config.enabled) return;

        if(!Discord.getInstance().checkChannel(config.channel, "onStart")) {
            config.enabled = false;
        }
    }

    @Override
    public void onServerStart() {
        if(!config.enabled) return;
        if(config.disableDuringMaintenance && maintenance.InMaintenance()) return;

        (new EmbedInfo())
            .setLanguage("events", "on-start")
            .sendInChannel(config.channel);
    }
}
