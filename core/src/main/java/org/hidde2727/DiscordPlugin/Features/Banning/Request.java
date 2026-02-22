package org.hidde2727.DiscordPlugin.Features.Banning;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.hidde2727.DiscordPlugin.Discord.*;
import org.hidde2727.DiscordPlugin.Logs;
import org.hidde2727.DiscordPlugin.Storage.Config;
import org.hidde2727.DiscordPlugin.Storage.DataStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

// First part of the whitelisting process, a whitelist request
public class Request {
    Discord discord;
    Banning banning;
    Config.Banning.Request config;

    static class AwaitingConfirmation {
        DataStorage.Player player;
        String reason;

        AwaitingConfirmation() {}
        AwaitingConfirmation(DataStorage.Player player, String reason) {
            this.player = player;
            this.reason = reason;
        }
    }
    private final Map<String, AwaitingConfirmation> awaitingConfirmation = new HashMap<>();// Discord user to request

    Request(Banning banning) {
        this.discord = banning.discord;
        this.banning = banning;
        this.config = banning.config.request;

        if(config.enabled && !discord.DoesTextChannelExist(config.channel)) {
            Logs.error("Banning request channel does not exist");
            banning.config.enabled = false;
        } else if(config.enabled && !discord.CanBotAccesTextChannel(config.channel)) {
            Logs.error("The bot cannot access the banning request channel");
            banning.config.enabled = false;
        }

        if(config.identifier.equals("discord") && !banning.players.ConnectAccounts()) {
            Logs.error("Cannot use the banning request identifier 'discord' if you do not connect discord accounts to minecraft accounts");
            config.enabled = false;
        }
        if(!config.identifier.equals("discord") && !config.identifier.equals("minecraft")) {
            Logs.error("Banning request must use either the 'discord' identifier or the 'minecraft' identifier. Not '" + config.identifier + "'");
            config.enabled = false;
        }
    }


    // 1. Initial embed people can use to get whitelisted:
    void SendRequestEmbed() {
        if(!config.enabled) return;
        discord.CreateEmbed()
                .SetLanguageNamespace("banning" , "request")
                .AddActionRow(new ActionRow(Button.Primary("ban-request-button", "button")))
                .DeleteOnShutdown()
                .SendInChannel(config.channel);
    }
    // 2. Someone pressed the button of 1, send modal to get the user they want to see punished
    void OnRequest(ButtonInteractionEvent event) {
        if(banning.HasRequest(event.getUser().getId())) {
            // The user already has a request
            discord.CreateEmbed()
                    .SetLanguageNamespace("banning","alreadyRequested")
                    .SetVariables(banning.GetVariables(event.getUser()))
                    .Send(event, true);
            return;
        }
        if(config.checkRoles && !discord.DoesUserHaveRoleInChannel(config.channel, event.getUser().getIdLong(), config.allowedRoles)) {
            // The user is not allowed to make a request
            discord.CreateEmbed()
                    .SetLanguageNamespace("banning","requestNotAllowed")
                    .SetVariables(banning.GetVariables(event.getUser()))
                    .Send(event, true);
            return;
        }
        Modal modal = discord.CreateModal("ban-request")
                .SetLanguageNamespace("banning", "request")
                .SetVariables(banning.GetVariables(event.getUser()));
        if(config.identifier.equals("discord")) {
            modal.Add(SelectMenu.Users("discord-user", "discordUser"));
        } else {
            modal.Add(SelectMenu.Custom("minecraft-user", "minecraftUser",
                    banning.permanentData.players.values().stream().collect(Collectors.toMap(
                            // Map key
                            (p) -> banning.players.GetMinecraftKey(p.minecraftName, p.minecraftUUID),
                            // Map value
                            (p) -> p.minecraftName
                    ))
            ));
        }
        modal.Add(TextField.Short("reason", "reason", 32, 1024));
        modal.Send(event);
    }
    // 3. Let the user check if they entered the correct username:
    void OnModalFinish(ModalInteractionEvent event) {
        String reason = event.getValue("reason").getAsString();

        DataStorage.Player player;
        if(config.identifier.equals("discord")) {
            Member discordUser = event.getValue("discord-user").getAsMentions().getMembers().get(0);
            player = banning.GetPlayerByDiscord(discordUser.getId());
            if(player == null) {
                discord.CreateEmbed()
                        .SetLanguageNamespace("banning", "playerNotFound")
                        .SetVariable("REASON", reason)
                        .SetVariables(banning.GetVariables(discordUser.getUser()))
                        .Send(event, true);
                return;// Ignore the request, as the user does not exist for us
            }
        } else {
            String minecraftKey = event.getValue("minecraft-user").getAsString();
            player = banning.GetPlayerByKey(minecraftKey);
        }
        awaitingConfirmation.put(event.getUser().getId(), new AwaitingConfirmation(player, reason));
        discord.CreateEmbed()
                .SetLanguageNamespace("banning", "requestConfirm")
                .SetVariable("REASON", reason)
                .SetVariables(banning.GetVariables(player))
                .AddActionRow(new ActionRow(
                        Button.Primary("ban-request-confirm-button", "confirm"),
                        Button.Destructive("ban-request-confirm-cancel", "cancel")
                ))
                .Send(event, true);
    }
    // The request was canceled
    void OnCancel(ButtonInteractionEvent event) {
        // awaitingConfirmation.remove(event.getUser().getId());
        discord.CreateEmbed()
                .SetLanguageNamespace("banning", "requestCanceled")
                .SetVariables(banning.GetVariables(event.getUser()))
                .Send(event, true);
    }
    // 4 Forward the request to banning
    void OnConfirmed(ButtonInteractionEvent event) {
        event.getMessage().delete().queue();
        String userId = event.getUser().getId();
        if(banning.HasRequest(userId)) return;
        AwaitingConfirmation request = awaitingConfirmation.get(userId);
        awaitingConfirmation.remove(userId);

        banning.AddRequest(event.getUser().getId(), request.player, request.reason);
        discord.CreateEmbed()
                .SetLanguageNamespace("banning", "requestConfirmed")
                .SetVariable("REASON", request.reason)
                .SetVariables(banning.GetVariables(request.player))
                .Send(event, true);
    }
}
