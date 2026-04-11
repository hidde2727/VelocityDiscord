package org.hidde2727.DiscordPlugin.Features;

import org.hidde2727.DiscordPlugin.Discord.*;
import org.hidde2727.DiscordPlugin.Storage.Config;
import org.hidde2727.DiscordPlugin.DiscordPlugin;
import org.hidde2727.DiscordPlugin.Logs;

import java.util.ArrayList;
import java.util.List;

public class SquashedRequest {
    private final Discord discord;
    private final Config config;

    public SquashedRequest(DiscordPlugin plugin) {
        this.discord = plugin.discord;
        this.config = plugin.config;

        if(!config.squashedRequest.enabled) return;

        if(!discord.DoesTextChannelExist(config.squashedRequest.channel)) {
            Logs.error("Squashed request channel does not exist");
            this.config.squashedRequest.enabled = false;
        } else if(!discord.CanBotAccesTextChannel(config.squashedRequest.channel)) {
            Logs.error("The bot cannot access the squashed request channel");
            this.config.squashedRequest.enabled = false;
        }

        config.whitelist.request.sendMessage = false;
        config.banning.request.sendMessage = false;
        config.unban.request.sendMessage = false;
    }

    public void OnServerStart() {
        if(!config.squashedRequest.enabled) return;

        List<ActionRowItem> buttons = new ArrayList<>();
        Embed embed = discord.CreateEmbed()
                .SetLanguageNamespace("squashedRequest", "request");
        if(config.whitelist.request.enabled) {
            buttons.add(Button.Primary("whitelist-request-button", "whitelist"));
        }
        if(config.banning.request.enabled) {
            buttons.add(Button.Destructive("ban-request-button", "ban"));
        }
        if(config.unban.request.enabled) {
            buttons.add(Button.Secondary("unban-request-button", "unban"));
        }
        if(buttons.isEmpty()) return;
        embed.AddActionRow(new ActionRow(buttons));
        embed.DeleteOnShutdown("squashedRequest.request");
        embed.SendInChannel(config.squashedRequest.channel);
    }
}
