package org.hidde2727.DiscordPlugin.Features.Whitelist;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.hidde2727.DiscordPlugin.Discord.ActionRow;
import org.hidde2727.DiscordPlugin.Discord.Button;
import org.hidde2727.DiscordPlugin.Discord.Discord;
import org.hidde2727.DiscordPlugin.Discord.TextField;
import org.hidde2727.DiscordPlugin.Logs;
import org.hidde2727.DiscordPlugin.Storage.Config;

import java.util.HashMap;
import java.util.Map;

// First part of the whitelisting process, a whitelist request
public class Request {
    Discord discord;
    Whitelist whitelist;
    Config.Whitelist.Request config;

    private final Map<String, String> awaitingConfirmation = new HashMap<>();// Discord user to minecraft username request

    Request(Whitelist whitelist) {
        this.discord = whitelist.discord;
        this.whitelist = whitelist;
        this.config = whitelist.config.request;

        if(config.enabled && !discord.DoesTextChannelExist(config.channel)) {
            Logs.error("Whitelist request channel does not exist");
            whitelist.config.enabled = false;
        } else if(config.enabled && !discord.CanBotAccesTextChannel(config.channel)) {
            Logs.error("The bot cannot access the whitelist request channel");
            whitelist.config.enabled = false;
        }
    }


    // 1. Initial embed people can use to get whitelisted:
    void SendRequestEmbed() {
        if(!config.enabled) return;
        discord.CreateEmbed()
                .SetLanguageNamespace("whitelist" , "request")
                .AddActionRow(new ActionRow(Button.Primary("whitelist-request-button", "button")))
                .DeleteOnShutdown()
                .SendInChannel(config.channel);
    }
    // 2. Someone pressed the button of 1, send modal to get their username
    void OnRequest(ButtonInteractionEvent event) {
        if(
            whitelist.HasRequest(event.getUser().getId()) ||
            (whitelist.players.ConnectAccounts() && whitelist.IsWhitelistedByDiscord(event.getUser().getId()))
        ) {
            // The user already has a request
            discord.CreateEmbed()
                    .SetLanguageNamespace("whitelist","alreadyWhitelisted")
                    .SetVariables(whitelist.GetVariables(event.getUser()))
                    .Send(event, true);
            return;
        }
        if(config.checkRoles && !discord.DoesUserHaveRoleInChannel(config.channel, event.getUser().getIdLong(), config.allowedRoles)) {
            // The user is not allowed to make a request
            discord.CreateEmbed()
                    .SetLanguageNamespace("whitelist","requestNotAllowed")
                    .SetVariables(whitelist.GetVariables(event.getUser()))
                    .Send(event, true);
            return;
        }
        discord.CreateModal("whitelist-request")
                .SetLanguageNamespace("whitelist", "request")
                .SetVariables(whitelist.GetVariables(event.getUser()))
                .Add(TextField.Short("username", "username", 3, 16))
                .Send(event);

    }
    // 3. Let the user check if they entered the correct username:
    void OnModalFinish(ModalInteractionEvent event) {
        String minecraftUsername = event.getValue("username").getAsString();
        awaitingConfirmation.put(event.getUser().getId(), minecraftUsername);
        discord.CreateEmbed()
                .SetLanguageNamespace("whitelist", "requestConfirm")
                .SetVariable("PLAYER_NAME", minecraftUsername)
                .SetVariables(whitelist.GetVariables(event.getUser()))
                .AddActionRow(new ActionRow(
                        Button.Primary("whitelist-request-confirm-button", "confirm"),
                        Button.Destructive("whitelist-request-confirm-cancel", "cancel")
                ))
                .Send(event, true);
    }
    // The request was canceled
    void OnCancel(ButtonInteractionEvent event) {
        // awaitingConfirmation.remove(event.getUser().getId());
        discord.CreateEmbed()
                .SetLanguageNamespace("whitelist", "requestCanceled")
                .SetVariables(whitelist.GetVariables(event.getUser()))
                .Send(event, true);
    }
    // 4 Either instant whitelist or get the request approved by admins:
    void OnConfirmed(ButtonInteractionEvent event) {
        event.getMessage().delete().queue();
        String userId = event.getUser().getId();
        if(
            whitelist.HasRequest(userId) ||
            (whitelist.players.ConnectAccounts() && whitelist.IsWhitelistedByDiscord(userId))
        ) return;
        String minecraftName = awaitingConfirmation.get(userId);
        awaitingConfirmation.remove(userId);

        if(whitelist.players.UseMinecraftUUID()) {
            // Get the players UUID from Mojang:
            event.deferReply(true);
            whitelist.GetMinecraftUUID(minecraftName).handleAsync((uuid, error) -> {
                if(error != null) {
                    event.deferEdit();
                    return false;
                }
                if(uuid == null) {
                    // Player name was not found
                    discord.CreateEmbed()
                            .SetLanguageNamespace("whitelist", "playerNotFound")
                            .SetVariable("PLAYER_NAME", minecraftName)
                            .SetVariables(whitelist.GetVariables(event.getUser()))
                            .Send(event, true);
                    return false;
                }
                OnConfirmed(event, minecraftName, uuid);
                return false;
            });
        } else {
            OnConfirmed(event, minecraftName, null);
        }
    }

    void OnConfirmed(ButtonInteractionEvent event, String minecraftName, String minecraftUUID) {
        whitelist.AddRequest(minecraftName, minecraftUUID, event.getUser().getId());
        discord.CreateEmbed()
                .SetLanguageNamespace("whitelist", "requestConfirmed")
                .SetVariable("PLAYER_NAME", minecraftName)
                .SetVariable("PLAYER_UUID", minecraftUUID)
                .SetVariable("PLAYER_KEY", whitelist.players.GetMinecraftKey(minecraftName, minecraftUUID))
                .SetVariables(whitelist.GetVariables(event.getUser()))
                .Send(event, true);
    }
}
