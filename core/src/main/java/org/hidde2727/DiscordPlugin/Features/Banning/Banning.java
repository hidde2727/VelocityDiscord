package org.hidde2727.DiscordPlugin.Features.Banning;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.hidde2727.DiscordPlugin.Discord.Discord;
import org.hidde2727.DiscordPlugin.DiscordPlugin;
import org.hidde2727.DiscordPlugin.Logs;
import org.hidde2727.DiscordPlugin.PlayerManager;
import org.hidde2727.DiscordPlugin.Storage.Config;
import org.hidde2727.DiscordPlugin.Storage.DataStorage;
import org.hidde2727.DiscordPlugin.Storage.DataStorage.Player;
import org.hidde2727.DiscordPlugin.Storage.Config.Banning.PunishmentPicker.PunishmentType;

import java.time.OffsetTime;
import java.util.HashMap;
import java.util.Map;

public class Banning extends ListenerAdapter {
    final Discord discord;
    final Config.Banning config;
    final DataStorage permanentData;
    final PlayerManager players;

    Request request;
    Punishment punishment;
    Reason reason;
    Result result;

    public Banning(DiscordPlugin plugin) {
        this.discord = plugin.discord;
        this.config = plugin.config.banning;
        this.permanentData = plugin.dataStorage;
        this.players = plugin.players;

        if(!config.enabled) return;
        if(config.giveRoleOnBan && !players.ConnectAccounts()) {
            Logs.error("Cannot give a banned player a role if connectAccounts=false");
            config.enabled = false;
            return;
        }

        this.request = new Request(this);
        this.punishment = new Punishment(this);
        this.reason = new Reason(this);
        this.result = new Result(this);
    }

    void AddRequest(String byDiscordUUID, Player player, String reason) {
        String minecraftKey = players.GetMinecraftKey(player.minecraftName, player.minecraftUUID);
        DataStorage.BanRequest request = new DataStorage.BanRequest(byDiscordUUID, reason, player, minecraftKey);
        permanentData.banRequests.put(minecraftKey, request);

        punishment.OnRequest(request);
    }
    void DecidePunishment(DataStorage.BanRequest request) {
        if(request.punishment == Config.Banning.PunishmentPicker.PunishmentType.None) {
            result.OnDeny(request);
            return;
        }
        permanentData.banRequests.remove(request.key);
        permanentData.banRequestsDecided.put(request.key, request);

        request.adminDiscordUUID = null;
        request.upVotes.clear();
        request.downVotes.clear();
        reason.OnRequest(request);
    }
    void OnDecideReason(DataStorage.BanRequest request) {
        permanentData.banRequestsDecided.remove(request.key);
        request.player.punishments.add(
                new Player.Punishment(request.punishment, request.punishmentName, request.duration, request.reason)
        );

        if(config.giveRoleOnBan) {
            if(!discord.GiveUserRole(request.player.discordUUID, config.bannedRoleID)) {
                Logs.warn("Cannot give a banned player a role that does not exist");
            }
        }

        result.OnAccept(request);
    }

    /***********************************************************************
     * Util
     ***********************************************************************/
    boolean HasRequest(String discordUUID) {
        for(DataStorage.BanRequest request : permanentData.banRequests.values()) {
            if(request.suggestedByDiscordUUID.equals(discordUUID)) return true;
        }
        return false;
    }
    Player GetPlayerByDiscord(String discordUUID) {
        return players.GetPlayerByDiscord(discordUUID);
    }
    Player GetPlayerByKey(String minecraftKey) {
        return players.GetPlayer(minecraftKey);
    }
    Map<String, String> GetVariables(User user) {
        Map<String, String> variables = new HashMap<>();
        if(user == null) return variables;
        variables.put("DISCORD_NAME", user.getName());
        variables.put("DISCORD_GLOBAL_NAME", user.getGlobalName());
        variables.put("DISCORD_EFFECTIVE_NAME", user.getEffectiveName());
        variables.put("DISCORD_UUID", user.getId());
        return variables;
    }
    Map<String, String> GetVariables(Player player) {
        Map<String, String> variables = GetVariables(discord.GetUserByID(player.discordUUID));
        variables.put("PLAYER_NAME", player.minecraftName);
        variables.put("PLAYER_UUID", player.minecraftUUID);
        variables.put("PLAYER_KEY", players.GetMinecraftKey(player.minecraftName, player.minecraftUUID));
        return variables;
    }
    Map<String, String> GetVariables(DataStorage.BanRequest request, boolean decided) {
        Map<String, String> variables = GetVariables(request.player);

        if(request.adminDiscordUUID != null) {
            User admin = discord.GetUserByID(request.adminDiscordUUID);
            variables.put("ADMIN_NAME", admin.getName());
            variables.put("ADMIN_GLOBAL_NAME", admin.getGlobalName());
            variables.put("ADMIN_EFFECTIVE_NAME", admin.getEffectiveName());
            variables.put("ADMIN_UUID", admin.getId());
        }

        variables.put("ACCEPT_VOTES", String.valueOf(request.upVotes.size()));
        variables.put("DENY_VOTES", String.valueOf(request.downVotes.size()));
        if(decided) {
            variables.put("MIN_ACCEPT_VOTES", String.valueOf(reason.NeededUpVotes()));
            variables.put("MIN_DENY_VOTES", String.valueOf(reason.NeededDownvote()));
        } else {
            variables.put("MIN_ACCEPT_VOTES", String.valueOf(punishment.NeededUpVotes()));
            variables.put("MIN_DENY_VOTES", String.valueOf(punishment.NeededDownvote()));
        }

        variables.put("ORIGINAL_REASON", request.originalReason);
        variables.put("REASON", request.reason);
        variables.put("PUNISHMENT", request.punishmentName);

        return variables;
    }

    /***********************************************************************
     * Events
     ***********************************************************************/

    public void OnServerStart() {
        request.SendRequestEmbed();
        punishment.SendVotingMessages();
        reason.SendVotingMessages();
    }

    public boolean OnPlayerPreLogin(String playerName, String playerUUID) {
        String minecraftKey = players.GetMinecraftKey(playerName, playerUUID);
        Player player = players.GetPlayer(minecraftKey);
        if(player == null) return false;
        for(Player.Punishment playerPunishment : player.punishments) {
            if(playerPunishment.punishment == PunishmentType.PermBan) return false;
            if(playerPunishment.punishment == PunishmentType.Kick) {
                playerPunishment.until = playerPunishment.until.minusSeconds(1);
                if(playerPunishment.until.isBefore(OffsetTime.MIN.plusSeconds(1))) player.punishments.remove(playerPunishment);
                return false;
            }
            if(playerPunishment.until.isBefore(OffsetTime.now())) player.punishments.remove(playerPunishment);
            else if(playerPunishment.punishment == PunishmentType.Ban) return false;
        }
        return player.whitelisted;
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if(event.getComponentId().equals("ban-punishment-selector")) {
            punishment.OnPunishmentPick(event);
        }
    }
    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (event.getComponentId().equals("ban-request-button")) {
            request.OnRequest(event);
        }
        else if (event.getComponentId().equals("ban-request-confirm-cancel")) {
            request.OnCancel(event);
        }
        else if (event.getComponentId().equals("ban-request-confirm-button")) {
            request.OnConfirmed(event);
        }
        else if(event.getComponentId().equals("ban-punishment-vote-up")) {
            punishment.OnUpVote(event);
        }
        else if(event.getComponentId().equals("ban-punishment-vote-down")) {
            punishment.OnDownVote(event);
        }
        else if(event.getComponentId().equals("ban-reason-vote-up")) {
            reason.OnUpVote(event);
        }
        else if(event.getComponentId().equals("ban-reason-vote-down")) {
            reason.OnDownVote(event);
        }
        else if(event.getComponentId().equals("ban-reason-change")) {
            reason.OnChangeButton(event);
        }
    }
    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (event.getModalId().equals("ban-request")) {
            request.OnModalFinish(event);
        } else if(event.getModalId().startsWith("banning-reason-change-")) {
            reason.OnReasonPick(event);
        }
    }

}