package org.hidde2727.VelocityDiscordPlugin.Features;

import org.hidde2727.VelocityDiscordPlugin.Config;
import org.hidde2727.VelocityDiscordPlugin.Logs;
import org.hidde2727.VelocityDiscordPlugin.Discord.Discord;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;

public class OnStart {
    private Discord discord;
    private Config.Events.OnStart config;

    public OnStart(Discord discord, Config.Events.OnStart config) {
        this.discord = discord;
        this.config = config;
        
        if(config.enabled && !discord.DoesTextChannelExist(config.channel)) {
            Logs.logger.error("onStart channel does not exist");
            this.config.enabled = false;
        } else if(config.enabled && !discord.CanBotAccesTextChannel(config.channel)) {
            Logs.logger.error("The bot cannot access the onStart channel");
            this.config.enabled = false;
        }
    }

    @Subscribe
    public void onInitialize(ProxyInitializeEvent event) {
        if(!config.enabled) return;
        discord.CreateEmbed()
            .SetLocalizationNamespace("embeds.onStart", 2)
            .SendInChannel(config.channel);
    }
}
