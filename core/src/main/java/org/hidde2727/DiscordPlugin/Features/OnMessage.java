package org.hidde2727.DiscordPlugin.Features;

import java.util.Map.Entry;

import org.hidde2727.DiscordPlugin.*;
import org.hidde2727.DiscordPlugin.Discord.EmbedInfo;
import org.hidde2727.DiscordPlugin.Models.Player;
import org.hidde2727.DiscordPlugin.Storage.Config;
import org.hidde2727.DiscordPlugin.Storage.DataStorage;
import org.hidde2727.DiscordPlugin.Discord.Discord;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class OnMessage extends Feature {
    private final Config.Events.OnMessage config;
    private final DataStorage.Maintenance maintenance;
    private ListenerAdapter listener;
    private final Implementation implementation;

    public OnMessage(Implementation implementation) {
        this.config = Config.getInstance().events.onMessage;
        this.maintenance = DataStorage.getInstance().maintenance;
        this.implementation = implementation;

        if(!config.enabled) return;
        if(config.channels.isEmpty()) {
            Logs.error("The onMessage event is enabled but has no channels configured");
            config.enabled = false;
            return;
        }
        for(Entry<String, String> channel : config.channels.entrySet()) {
            if(!Discord.getInstance().checkChannel(channel.getValue(), "onMessage '" + channel.getKey() + "'")) {
                config.enabled = false;
            }
        }

        if(config.discordToMinecraft) {
            OnMessage self = this;
            this.listener = new ListenerAdapter() {
                @Override
                public void onMessageReceived(@NotNull MessageReceivedEvent event) {
                    self.onMessageReceived(event);
                }
            };
        }
    }

    @Override
    public void onPlayerMessage(String onServer, Player player, String message) {
        if(!config.enabled) return;
        if(!config.minecraftToDiscord) return;
        if(config.disableDuringMaintenance && maintenance.InMaintenance()) return;

        (new EmbedInfo())
            .setLanguage("events", "on-message")
            .setVariables(new CombinedVariableProvider(player, new SingleVariableProvider("MESSAGE", message)))
            .sendInChannel(config.channels.get(onServer));
    }

    public void onMessageReceived(MessageReceivedEvent event) {
        if(!config.enabled) return;
        if(!config.discordToMinecraft) return;
        if(config.disableDuringMaintenance && maintenance.InMaintenance()) return;
        String botId = Discord.getInstance().getSelfId();
        String authorId = event.getAuthor().getId();
        if(authorId.equals(botId)) return;// Make sure to not create an infinite loop

        // Find the server to send the message to:
        // (Check if the message is sent in a channel in the config)
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
        implementation.sendMessage(serverID, event.getMessage().getContentStripped());
    }
}
