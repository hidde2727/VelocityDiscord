package org.hidde2727.DiscordPlugin.Features.Unban;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IModalCallback;
import org.hidde2727.DiscordPlugin.Discord.*;
import org.hidde2727.DiscordPlugin.Logs;
import org.hidde2727.DiscordPlugin.PlayerManager;
import org.hidde2727.DiscordPlugin.Storage.Config;
import org.hidde2727.DiscordPlugin.Storage.DataStorage;
import org.hidde2727.DiscordPlugin.Storage.DataStorage.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// First part of the whitelisting process, a whitelist request
public class Request {
    Discord discord;
    PlayerManager players;
    Unban unban;
    Config.Unban.Request config;

    static class AwaitingConfirmation {
        DataStorage.Player player;
        String reason;
        Player.Punishment forPunishment;

        AwaitingConfirmation() {}
        AwaitingConfirmation(DataStorage.Player player, String reason) {
            this.player = player;
            this.reason = reason;
        }
    }
    private final Map<String, AwaitingConfirmation> awaitingConfirmation = new HashMap<>();// Discord user to request

    Request(Unban unban) {
        this.discord = unban.discord;
        this.players = unban.players;
        this.unban = unban;
        this.config = unban.config.request;

        if(!config.enabled) return;

        if(config.sendMessage && !discord.DoesTextChannelExist(config.channel)) {
            Logs.error("Unban request channel does not exist");
            unban.config.enabled = false;
        } else if(config.sendMessage && !discord.CanBotAccesTextChannel(config.channel)) {
            Logs.error("The bot cannot access the unban request channel");
            unban.config.enabled = false;
        }
    }


    // 1. Initial embed people can use to get whitelisted:
    void SendRequestEmbed() {
        if(!config.enabled) return;
        if(!config.sendMessage) return;
        discord.CreateEmbed()
                .SetLanguageNamespace("unban" , "request")
                .AddActionRow(new ActionRow(Button.Secondary("unban-request-button", "button")))
                .DeleteOnShutdown()
                .SendInChannel(config.channel);
    }
    // 2. Someone pressed the button of 1, send modal to get the user they want to see punished
    void OnRequest(ButtonInteractionEvent event) {
        if(!unban.players.ConnectAccounts()) {
            if(!CheckUser(event, null)) return;
            // Accounts aren't connected, so we do not know which miencraft user the person wants unbanned:
            discord.CreateEmbed()
                .SetLanguageNamespace("unban", "requestPlayerPicker")
                .SetVariables(unban.GetVariables(event.getUser()))
                .AddActionRow(new ActionRow(SelectMenu.Custom("unban-request-player-selector", "minecraftUser",
                    unban.permanentData.players.values().stream().collect(Collectors.toMap(
                            // Map key
                            (p) -> unban.players.GetMinecraftKey(p.minecraftName, p.minecraftUUID),
                            // Map value
                            (p) -> p.minecraftName
                    ))
                )))
                .Send(event, true);
        } else {
            Player player = unban.GetPlayerByDiscord(event.getUser().getId());
            if(player == null) {
                discord.CreateEmbed()
                        .SetLanguageNamespace("unban", "playerNotFound")
                        .SetVariables(unban.GetVariables(event.getUser()))
                        .Send(event, true);
                return;
            }
            if(!CheckUser(event, player)) return;
            OnPlayerPick(player, event);
        }

    }
    // 2.1 The player picked which minecraft player they want to see unbanned
    void OnPlayerPickStringSelect(StringSelectInteractionEvent event) {
        event.getMessage().delete().queue();
        String minecraftKey = event.getInteraction().getValues().getFirst();
        Player player = unban.GetPlayerByKey(minecraftKey);
        if(player.punishments.isEmpty()) {
            // The user isn't banned
            discord.CreateEmbed()
                    .SetLanguageNamespace("unban", "notBanned")
                    .SetVariables(unban.GetVariables(event.getUser()))
                    .Send(event, true);
            return;
        }

        OnPlayerPick(player, event);
    }
    // 2.2 Send the reason picker modal
    void OnPlayerPick(Player player, IModalCallback event) {
        awaitingConfirmation.put(event.getUser().getId(), new AwaitingConfirmation(player, null));
        discord.CreateModal("unban-request")
            .SetLanguageNamespace("unban", "request")
            .SetVariables(unban.GetVariables(event.getUser()))
            .Add(SelectMenu.Custom("punishment", "punishment",
                IntStream.range(0, player.punishments.size())
                    .boxed()
                    .collect(Collectors.toMap(
                        (i) -> Integer.toString(i),
                        (i) -> player.punishments.get(i).punishmentName
                    ))
            ))
            .Add(TextField.Short("reason", "reason", 32, 1024))
            .Send(event);
    }
    // 3. Let the user check if they entered the correct user and reason:
    void OnModalFinish(ModalInteractionEvent event) {
        AwaitingConfirmation confirm = awaitingConfirmation.get(event.getUser().getId());
        if(confirm == null) {
            Logs.warn("Unban.Request.OnModalFinish got called for a user that does not have a request");
            return;
        }

        String reason = event.getValue("reason").getAsString();
        Player player = confirm.player;

        confirm.reason = reason;
        confirm.forPunishment = confirm.player.punishments.get(
                Integer.parseInt(event.getValue("punishment").getAsStringList().get(0))
        );

        String punishmentName = confirm.forPunishment.punishmentName;

        discord.CreateEmbed()
                .SetLanguageNamespace("unban", "requestConfirm")
                .SetVariable("REASON", reason)
                .SetVariable("PUNISHMENT_NAME", punishmentName)
                .SetVariables(unban.GetVariables(player))
                .AddActionRow(new ActionRow(
                        Button.Primary("unban-request-confirm-button", "confirm"),
                        Button.Destructive("unban-request-confirm-cancel", "cancel")
                ))
                .Send(event, true);
    }
    // The request was canceled
    void OnCancel(ButtonInteractionEvent event) {
        awaitingConfirmation.remove(event.getUser().getId());
        event.getMessage().delete().queue();
        discord.CreateEmbed()
                .SetLanguageNamespace("unban", "requestCanceled")
                .SetVariables(unban.GetVariables(event.getUser()))
                .Send(event, true);
    }
    // 4 Forward the request to unban
    void OnConfirmed(ButtonInteractionEvent event) {
        String userId = event.getUser().getId();
        AwaitingConfirmation request = awaitingConfirmation.get(userId);
        if(request == null) {
            Logs.warn("Unban.Request.OnConfirmed called for a user without a request");
            return;
        }
        awaitingConfirmation.remove(userId);
        if(!CheckUser(event, request.player)) return;

        event.getMessage().delete().queue();

        unban.AddRequest(event.getUser().getId(), request.player, request.reason, request.forPunishment);
        discord.CreateEmbed()
                .SetLanguageNamespace("unban", "requestConfirmed")
                .SetVariable("REASON", request.reason)
                .SetVariables(unban.GetVariables(request.player))
                .Send(event, true);
    }

    private boolean CheckUser(ButtonInteractionEvent event, Player player) {
        if(!config.enabled) {
            event.deferEdit().queue();
            return false;
        }

        if(unban.HasRequest(event.getUser().getId())) {
            // The user already has a request
            discord.CreateEmbed()
                    .SetLanguageNamespace("unban", "alreadyRequested")
                    .SetVariables(unban.GetVariables(event.getUser()))
                    .Send(event, true);
            return false;
        }
        if(unban.players.ConnectAccounts() && !unban.IsDiscordUUIDBanned(event.getUser().getId())) {
            // The user isn't banned
            discord.CreateEmbed()
                    .SetLanguageNamespace("unban", "notBanned")
                    .SetVariables(unban.GetVariables(event.getUser()))
                    .Send(event, true);
            return false;
        }
        if(config.checkRoles && !discord.DoesUserHaveRole(event.getUser(), config.allowedRoles)) {
            // The user is not allowed to make a request
            discord.CreateEmbed()
                    .SetLanguageNamespace("unban","requestNotAllowed")
                    .SetVariables(unban.GetVariables(event.getUser()))
                    .Send(event, true);
            return false;
        }
        if(player != null) {
            if(players.HasPunishment(player, Config.Banning.PunishmentPicker.PunishmentType.NoUnban)) {
                discord.CreateEmbed()
                        .SetLanguageNamespace("unban","noUnbanPunishment")
                        .SetVariables(unban.GetVariables(event.getUser()))
                        .SetVariables(unban.GetVariables(players.GetPunishment(player, Config.Banning.PunishmentPicker.PunishmentType.NoUnban)))
                        .Send(event, true);
                return false;
            }
        }
        return true;
    }
}
