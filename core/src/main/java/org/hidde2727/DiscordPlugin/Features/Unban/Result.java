package org.hidde2727.DiscordPlugin.Features.Unban;

import org.hidde2727.DiscordPlugin.Discord.Discord;
import org.hidde2727.DiscordPlugin.Discord.Embed;
import org.hidde2727.DiscordPlugin.Logs;
import org.hidde2727.DiscordPlugin.Storage.Config;
import org.hidde2727.DiscordPlugin.Storage.DataStorage;

public class Result {
    Unban unban;
    Config.Unban config;
    Discord discord;

    Result(Unban unban) {
        this.unban = unban;
        this.config = unban.config;
        this.discord = unban.discord;

        if(config.onAccept.enabled && !discord.DoesTextChannelExist(config.onAccept.channel)) {
            Logs.error("Unban onAccept channel does not exist");
            unban.config.enabled = false;
        } else if(config.onAccept.enabled && !discord.CanBotAccesTextChannel(config.onAccept.channel)) {
            Logs.error("The bot cannot access the unban onAccept channel");
            unban.config.enabled = false;
        }

        if(config.onDeny.enabled && !discord.DoesTextChannelExist(config.onDeny.channel)) {
            Logs.error("Unban onDeny channel does not exist");
            unban.config.enabled = false;
        } else if(config.onDeny.enabled && !discord.CanBotAccesTextChannel(config.onDeny.channel)) {
            Logs.error("The bot cannot access the unban onDeny channel");
            unban.config.enabled = false;
        }
    }



    // Last step, whitelist and announce the whitelist
    void OnAccept(DataStorage.UnbanRequest request) {
        // Send the public whitelisted message:
        if(!config.onAccept.enabled) return;
        Embed embed = discord.CreateEmbed()
                .SetLanguageNamespace("unban", "publicAccepted")
                .SetVariables(unban.GetVariables(request));
        embed.SendInChannel(config.onAccept.channel);
    }
    // Last step, deny the whitelist and announce it
    void OnDeny(DataStorage.UnbanRequest request) {
        // Send the public whitelist deny message:
        if(!config.onDeny.enabled) return;
        discord.CreateEmbed()
                .SetLanguageNamespace("unban", "publicDenied")
                .SetVariables(unban.GetVariables(request))
                .SendInChannel(config.onDeny.channel);
    }
}
