package org.hidde2727.VelocityDiscordPlugin.Features;

import java.util.Map.Entry;

import org.hidde2727.VelocityDiscordPlugin.Config;
import org.hidde2727.VelocityDiscordPlugin.Logs;
import org.hidde2727.VelocityDiscordPlugin.Discord.Discord;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class OnMessage extends ListenerAdapter {
    Discord discord;
    Config.Events.OnMessage config;

    public OnMessage(Discord discord, Config.Events.OnMessage config) {
        this.discord = discord;
        this.config = config;

        if(!config.enabled) return;
        for(Entry<String, String> channel : config.channels.entrySet()) {
            if(!discord.DoesTextChannelExist(channel.getValue())) {
                Logs.logger.error("onMessage '" + channel.getKey() + "' channel does not exist");
                this.config.enabled = false;
            } else if(config.enabled && !discord.CanBotAccesTextChannel(channel.getValue())) {
                Logs.logger.error("The bot cannot access the onMessage '" + channel.getKey() + "' channelt");
                this.config.enabled = false;
            }
        }
    }

    @Subscribe
    public void onChat(PlayerChatEvent event) {
        if(!config.enabled) return;
        String onServer = event.getPlayer().getCurrentServer().get().getServerInfo().getName();
        discord.CreateEmbed()
            .SetLocalizationNamespace("embeds.onMessage", 2)
            .SetVariable("PLAYER_NAME", event.getPlayer().getUsername())
            .SetVariable("PLAYER_UUID", event.getPlayer().getUniqueId().toString())
            .SetVariable("PLAYER_SERVER", onServer)
            .SetVariable("MESSAGE", event.getMessage())
            .SendInChannel(config.channels.get(onServer));
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if(!config.enabled) return;
        // Send a minecraft message:

    }
}
