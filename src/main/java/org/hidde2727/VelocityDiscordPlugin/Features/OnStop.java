package org.hidde2727.VelocityDiscordPlugin.Features;

import org.hidde2727.VelocityDiscordPlugin.Config;
import org.hidde2727.VelocityDiscordPlugin.Logs;
import org.hidde2727.VelocityDiscordPlugin.Discord.Discord;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;

public class OnStop {
    private Discord discord;
    private Config.Events.OnStop config;

    public OnStop(Discord discord, Config.Events.OnStop config) {
        this.discord = discord;
        this.config = config;
        
        if(config.enabled && !discord.DoesTextChannelExist(config.channel)) {
            Logs.logger.error("onStop channel does not exist");
            this.config.enabled = false;
        } else if(config.enabled && !discord.CanBotAccesTextChannel(config.channel)) {
            Logs.logger.error("The bot cannot access the onStop channel");
            this.config.enabled = false;
        }
    }

    @Subscribe
    public void OnShutdown(ProxyShutdownEvent event) {
        if(!config.enabled) return;
        discord.CreateEmbed()
            .SetLocalizationNamespace("embeds.onStop", 2)
            .SendInChannel(config.channel);
    }
}
