package org.hidde2727.DiscordPlugin.Features.Banning;

import org.hidde2727.DiscordPlugin.Discord.Discord;
import org.hidde2727.DiscordPlugin.Discord.Embed;
import org.hidde2727.DiscordPlugin.Logs;
import org.hidde2727.DiscordPlugin.Storage.Config;
import org.hidde2727.DiscordPlugin.Storage.DataStorage;

public class Result {
    Banning banning;
    Config.Banning config;
    Discord discord;

    Result(Banning banning) {
        this.banning = banning;
        this.config = banning.config;
        this.discord = banning.discord;

        if(config.onAccept.enabled && !discord.DoesTextChannelExist(config.onAccept.channel)) {
            Logs.error("Banning onAccept channel does not exist");
            banning.config.enabled = false;
        } else if(config.onAccept.enabled && !discord.CanBotAccesTextChannel(config.onAccept.channel)) {
            Logs.error("The bot cannot access the banning onAccept channel");
            banning.config.enabled = false;
        }

        if(config.onDeny.enabled && !discord.DoesTextChannelExist(config.onDeny.channel)) {
            Logs.error("Banning onDeny channel does not exist");
            banning.config.enabled = false;
        } else if(config.onDeny.enabled && !discord.CanBotAccesTextChannel(config.onDeny.channel)) {
            Logs.error("The bot cannot access the banning onDeny channel");
            banning.config.enabled = false;
        }
    }



    // Last step, whitelist and announce the whitelist
    void OnAccept(DataStorage.BanRequest request) {
        // Send the public whitelisted message:
        if(!config.onAccept.enabled) return;
        Embed embed = discord.CreateEmbed()
                .SetLanguageNamespace("banning", "publicAccepted")
                .SetVariables(banning.GetVariables(request, true));
        embed.SendInChannel(config.onAccept.channel);
    }
    // Last step, deny the whitelist and announce it
    void OnDeny(DataStorage.BanRequest request) {
        // Send the public whitelist deny message:
        if(!config.onDeny.enabled) return;
        discord.CreateEmbed()
                .SetLanguageNamespace("banning", "publicDenied")
                .SetVariables(banning.GetVariables(request, true))
                .SendInChannel(config.onDeny.channel);
    }
}
