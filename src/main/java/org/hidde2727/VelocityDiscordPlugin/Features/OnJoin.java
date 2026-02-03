package org.hidde2727.VelocityDiscordPlugin.Features;

import org.hidde2727.VelocityDiscordPlugin.Config;
import org.hidde2727.VelocityDiscordPlugin.Logs;
import org.hidde2727.VelocityDiscordPlugin.Discord.Discord;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerConnectedEvent;

public class OnJoin {
    Discord discord;
    Config.Events.OnJoin config;

    public OnJoin(Discord discord, Config.Events.OnJoin config) {
        this.discord = discord;
        this.config = config;

        if(config.enabled && !discord.DoesTextChannelExist(config.channel)) {
            Logs.logger.error("onJoin channel does not exist");
            this.config.enabled = false;
        } else if(config.enabled && !discord.CanBotAccesTextChannel(config.channel)) {
            Logs.logger.error("The bot cannot access the onJoin channel");
            this.config.enabled = false;
        }
    }
    
    @Subscribe
    void onConnect(ServerConnectedEvent event) {
        if(!config.enabled) return;
        discord.CreateEmbed()
            .SetLocalizationNamespace("embeds.onJoin", 2)
            .SetVariable("PLAYER_NAME", event.getPlayer().getUsername())
            .SetVariable("PLAYER_UUID", event.getPlayer().getUniqueId().toString())
            .SendInChannel(config.channel);
    }
}
