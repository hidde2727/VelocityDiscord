package org.hidde2727.DiscordPlugin.Features;

import org.hidde2727.DiscordPlugin.Discord.EmbedInfo;
import org.hidde2727.DiscordPlugin.Feature;
import org.hidde2727.DiscordPlugin.Storage.Config;
import org.hidde2727.DiscordPlugin.Storage.DataStorage;
import org.hidde2727.DiscordPlugin.Discord.Discord;

public class OnStop extends Feature {
    private final Config.Events.OnStop config;
    private final DataStorage.Maintenance maintenance;

    public OnStop() {
        this.config = Config.getInstance().events.onStop;
        this.maintenance = DataStorage.getInstance().maintenance;

        if(!config.enabled) return;

        if(Discord.getInstance().checkChannel(config.channel, "onStop")) {
            config.enabled = false;
        }
    }

    @Override
    public void onServerStop() {
        if(!config.enabled) return;
        if(config.disableDuringMaintenance && maintenance.InMaintenance()) return;

        (new EmbedInfo())
            .setLanguage("events", "on-stop")
            .sendInChannel(config.channel);
    }
}
