package org.hidde2727.DiscordPlugin.Features;

import org.hidde2727.DiscordPlugin.Storage.Config;
import org.hidde2727.DiscordPlugin.Storage.DataStorage;
import org.hidde2727.DiscordPlugin.DiscordPlugin;
import org.hidde2727.DiscordPlugin.Logs;
import org.hidde2727.DiscordPlugin.Discord.Discord;

public class OnStop {
    private Discord discord;
    private Config.Events.OnStop config;

    DataStorage.Maintenance maintenance;

    public OnStop(DiscordPlugin plugin) {
        this.discord = plugin.discord;
        this.config = plugin.config.events.onStop;
        this.maintenance = plugin.dataStorage.maintenance;
        
        if(config.enabled && !discord.DoesTextChannelExist(config.channel)) {
            Logs.error("onStop channel does not exist");
            this.config.enabled = false;
        } else if(config.enabled && !discord.CanBotAccesTextChannel(config.channel)) {
            Logs.error("The bot cannot access the onStop channel");
            this.config.enabled = false;
        }
    }

    public void OnServerStop() {
        if(!config.enabled) return;
        if(config.disableDuringMaintenance && maintenance.InMaintenance()) return;

        discord.CreateEmbed()
            .SetLanguageNamespace("events", "onStop")
            .SendInChannel(config.channel);
    }
}
