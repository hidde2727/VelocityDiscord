package org.hidde2727.DiscordPlugin.Features;

import org.hidde2727.DiscordPlugin.Config;
import org.hidde2727.DiscordPlugin.DataStorage;
import org.hidde2727.DiscordPlugin.DiscordPlugin;
import org.hidde2727.DiscordPlugin.Logs;
import org.hidde2727.DiscordPlugin.Discord.Discord;

public class OnStart {
    private Discord discord;
    private Config.Events.OnStart config;

    DataStorage.Maintenance maintenance;

    public OnStart(DiscordPlugin plugin) {
        this.discord = plugin.discord;
        this.config = plugin.config.events.onStart;
        this.maintenance = plugin.dataStorage.maintenance;
        
        if(config.enabled && !discord.DoesTextChannelExist(config.channel)) {
            Logs.error("onStart channel does not exist");
            this.config.enabled = false;
        } else if(config.enabled && !discord.CanBotAccesTextChannel(config.channel)) {
            Logs.error("The bot cannot access the onStart channel");
            this.config.enabled = false;
        }
    }

    public void OnServerStart() {
        if(!config.enabled) return;
        if(maintenance.InMaintenance()) return;

        discord.CreateEmbed()
            .SetLocalizationNamespace("embeds.onStart", 2)
            .SendInChannel(config.channel);
    }
}
