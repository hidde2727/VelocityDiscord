package org.hidde2727.VelocityDiscordPlugin.Features;

import org.hidde2727.VelocityDiscordPlugin.Config;
import org.hidde2727.VelocityDiscordPlugin.Discord.Discord;
import org.slf4j.Logger;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;

public class OnLeave {
    Discord discord;
    Config.Events.OnLeave config;

    public OnLeave(Discord discord, Config.Events.OnLeave config, Logger logger) {
        this.discord = discord;
        this.config = config;

        if(config.enabled && !discord.DoesTextChannelExist(config.channel)) {
            logger.error("onLeave channel does not exist");
            this.config.enabled = false;
        }
    }
    
    @Subscribe
    void onDisconnect(DisconnectEvent event) {
        if(!config.enabled) return;
        discord.CreateEmbed()
            .SetLocalizationNamespace("embeds.onLeave", 2)
            .SetVariable("PLAYER_NAME", event.getPlayer().getUsername())
            .SetVariable("PLAYER_UUID", event.getPlayer().getUniqueId().toString())
            .SendInChannel(config.channel);
    }
}
