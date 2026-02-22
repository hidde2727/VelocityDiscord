package org.hidde2727.DiscordPlugin.Features.Whitelist;

import org.hidde2727.DiscordPlugin.Discord.Discord;
import org.hidde2727.DiscordPlugin.Discord.Embed;
import org.hidde2727.DiscordPlugin.Logs;
import org.hidde2727.DiscordPlugin.Storage.Config;
import org.hidde2727.DiscordPlugin.Storage.DataStorage;

public class Result {
    Whitelist whitelist;
    Config.Whitelist config;
    Discord discord;

    Result(Whitelist whitelist) {
        this.whitelist = whitelist;
        this.config = whitelist.config;
        this.discord = whitelist.discord;

        if(config.onAccept.enabled && !discord.DoesTextChannelExist(config.onAccept.channel)) {
            Logs.error("Whitelist onAccept channel does not exist");
            whitelist.config.enabled = false;
        } else if(config.onAccept.enabled && !discord.CanBotAccesTextChannel(config.onAccept.channel)) {
            Logs.error("The bot cannot access the whitelist onAccept channel");
            whitelist.config.enabled = false;
        }

        if(config.onDeny.enabled && !discord.DoesTextChannelExist(config.onDeny.channel)) {
            Logs.error("Whitelist onDeny channel does not exist");
            whitelist.config.enabled = false;
        } else if(config.onDeny.enabled && !discord.CanBotAccesTextChannel(config.onDeny.channel)) {
            Logs.error("The bot cannot access the whitelist onDeny channel");
            whitelist.config.enabled = false;
        }
    }



    // Last step, whitelist and announce the whitelist
    void OnAccept(DataStorage.WhitelistRequest request) {
        // Send the public whitelisted message:
        if(!config.onAccept.enabled) return;
        Embed embed = discord.CreateEmbed()
                .SetLanguageNamespace("whitelist", "publicAccepted")
                .SetVariables(whitelist.GetVariables(request));
        embed.SendInChannel(config.onAccept.channel);
    }
    // Last step, deny the whitelist and announce it
    void OnDeny(DataStorage.WhitelistRequest request) {
        // Send the public whitelist deny message:
        if(!config.onDeny.enabled) return;
        discord.CreateEmbed()
                .SetLanguageNamespace("whitelist", "publicDenied")
                .SetVariables(whitelist.GetVariables(request))
                .SendInChannel(config.onDeny.channel);
    }
}
