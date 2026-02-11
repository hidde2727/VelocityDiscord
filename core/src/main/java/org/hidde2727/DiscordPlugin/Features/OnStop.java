package org.hidde2727.DiscordPlugin.Features;

import org.hidde2727.DiscordPlugin.Config;
import org.hidde2727.DiscordPlugin.DiscordPlugin;
import org.hidde2727.DiscordPlugin.Logs;
import org.hidde2727.DiscordPlugin.Discord.Discord;

public class OnStop {
    private Discord discord;
    private Config.Events.OnStop config;

    public OnStop(DiscordPlugin plugin) {
        this.discord = plugin.discord;
        this.config = plugin.config.events.onStop;
        
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
        discord.CreateEmbed()
            .SetLocalizationNamespace("embeds.onStop", 2)
            .SendInChannel(config.channel);
    }
}
