package org.hidde2727.DiscordPlugin.Features;

import org.hidde2727.DiscordPlugin.Config;
import org.hidde2727.DiscordPlugin.Logs;
import org.hidde2727.DiscordPlugin.Discord.Discord;

public class OnLeave {
    Discord discord;
    Config.Events.OnLeave config;

    public OnLeave(Discord discord, Config.Events.OnLeave config) {
        this.discord = discord;
        this.config = config;

        if(config.enabled && !discord.DoesTextChannelExist(config.channel)) {
            Logs.error("onLeave channel does not exist");
            this.config.enabled = false;
        } else if(config.enabled && !discord.CanBotAccesTextChannel(config.channel)) {
            Logs.error("The bot cannot access the onLeave channel");
            this.config.enabled = false;
        }
    }
    
    public void OnPlayerDisconnect(String playerName, String playerUUID) {
        if(!config.enabled) return;
        discord.CreateEmbed()
            .SetLocalizationNamespace("embeds.onLeave", 2)
            .SetVariable("PLAYER_NAME", playerName)
            .SetVariable("PLAYER_UUID", playerUUID)
            .SendInChannel(config.channel);
    }
}
