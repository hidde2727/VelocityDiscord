package org.hidde2727.DiscordPlugin.Features;

import org.hidde2727.DiscordPlugin.Config;
import org.hidde2727.DiscordPlugin.DataStorage;
import org.hidde2727.DiscordPlugin.DiscordPlugin;
import org.hidde2727.DiscordPlugin.Logs;
import org.hidde2727.DiscordPlugin.Discord.Discord;

public class OnJoin {
    Discord discord;
    Config.Events.OnJoin config;

    DataStorage.Maintenance maintenance;

    public OnJoin(DiscordPlugin plugin) {
        this.discord = plugin.discord;
        this.config = plugin.config.events.onJoin;
        this.maintenance = plugin.dataStorage.maintenance;

        if(config.enabled && !discord.DoesTextChannelExist(config.channel)) {
            Logs.error("onJoin channel does not exist");
            this.config.enabled = false;
        } else if(config.enabled && !discord.CanBotAccesTextChannel(config.channel)) {
            Logs.error("The bot cannot access the onJoin channel");
            this.config.enabled = false;
        }
    }
    
    public void OnPlayerConnect(String playerName, String playerUUID) {
        if(!config.enabled) return;
        if(maintenance.InMaintenance()) return;

        discord.CreateEmbed()
            .SetLocalizationNamespace("embeds.onJoin", 2)
            .SetVariable("PLAYER_NAME", playerName)
            .SetVariable("PLAYER_UUID", playerUUID)
            .SendInChannel(config.channel);
    }
}
