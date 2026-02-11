package org.hidde2727.DiscordPlugin.Features;

import java.util.Map.Entry;

import org.hidde2727.DiscordPlugin.Config;
import org.hidde2727.DiscordPlugin.DiscordPlugin;
import org.hidde2727.DiscordPlugin.Logs;
import org.hidde2727.DiscordPlugin.Discord.Discord;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class OnMessage extends ListenerAdapter {
    Discord discord;
    Config.Events.OnMessage config;
    DiscordPlugin plugin;

    public OnMessage(DiscordPlugin plugin) {
        this.discord = plugin.discord;
        this.config = plugin.config.events.onMessage;
        this.plugin = plugin;

        if(!config.enabled) return;
        for(Entry<String, String> channel : config.channels.entrySet()) {
            if(!discord.DoesTextChannelExist(channel.getValue())) {
                Logs.error("onMessage '" + channel.getKey() + "' channel does not exist");
                this.config.enabled = false;
            } else if(config.enabled && !discord.CanBotAccesTextChannel(channel.getValue())) {
                Logs.error("The bot cannot access the onMessage '" + channel.getKey() + "' channelt");
                this.config.enabled = false;
            }
        }
    }

    public void OnPlayerMessage(String onServer, String playerName, String playerUUID, String message) {
        if(!config.enabled) return;
        if(!config.minecraftToDiscord) return;
        discord.CreateEmbed()
            .SetLocalizationNamespace("embeds.onMessage", 2)
            .SetVariable("PLAYER_NAME", playerName)
            .SetVariable("PLAYER_UUID", playerUUID)
            .SetVariable("PLAYER_SERVER", onServer)
            .SetVariable("MESSAGE", message)
            .SendInChannel(config.channels.get(onServer));
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if(!config.enabled) return;
        if(!config.discordToMinecraft) return;
        if(event.getAuthor().getId().equals(discord.GetSelfId())) return;// Make sure to not create an infinite loop
        // Find the channel to user:
        String channelID = event.getChannel().getId();
        String serverID = null;
        for(Entry<String, String> entry : config.channels.entrySet()) {
            if(entry.getValue().equals(channelID)) {
                serverID = entry.getKey();
                break;
            }
        }
        if(serverID == null) {
            // Not for us
            return;
        }
        plugin.implementation.SendMessage(serverID, event.getMessage().getContentStripped());
    }
}
