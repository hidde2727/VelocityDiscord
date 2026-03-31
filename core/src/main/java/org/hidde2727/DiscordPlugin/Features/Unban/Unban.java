package org.hidde2727.DiscordPlugin.Features.Unban;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.hidde2727.DiscordPlugin.Discord.Discord;
import org.hidde2727.DiscordPlugin.DiscordPlugin;
import org.hidde2727.DiscordPlugin.PlayerManager;
import org.hidde2727.DiscordPlugin.Storage.Config;
import org.hidde2727.DiscordPlugin.Storage.DataStorage;
import org.hidde2727.DiscordPlugin.Storage.DataStorage.Player;

import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Unban extends ListenerAdapter {
    final Discord discord;
    final Config.Unban config;
    final DataStorage permanentData;
    final PlayerManager players;

    Request request;
    Voting voting;
    Result result;

    public Unban(DiscordPlugin plugin) {
        this.discord = plugin.discord;
        this.config = plugin.config.unban;
        this.permanentData = plugin.dataStorage;
        this.players = plugin.players;

        if(!config.enabled) return;

        this.request = new Request(this);
        this.voting = new Voting(this);
        this.result = new Result(this);
    }

    void AddRequest(String byDiscordUUID, Player player, String reason, Player.Punishment forPunishment) {
        String minecraftKey = players.GetMinecraftKey(player.minecraftName, player.minecraftUUID);
        DataStorage.UnbanRequest request = new DataStorage.UnbanRequest(byDiscordUUID, reason, player, minecraftKey, forPunishment);
        permanentData.unbanRequests.put(minecraftKey, request);

        voting.OnRequest(request);
    }
    void AcceptUnban(DataStorage.UnbanRequest request) {
        permanentData.unbanRequests.remove(request.key);
        result.OnAccept(request);
        request.player.punishments.remove(request.forPunishment);
        players.RemovePlayerRoleIfNoPunishments(request.player);
    }
    void DenyUnban(DataStorage.UnbanRequest request) {
        permanentData.unbanRequests.remove(request.key);
        result.OnDeny(request);
    }

    /***********************************************************************
     * Util
     ***********************************************************************/
    boolean IsDiscordUUIDBanned(String discordUUID) {
        return !GetPlayerByDiscord(discordUUID).punishments.isEmpty();
    }
    boolean HasRequest(String discordUUID) {
        for(DataStorage.UnbanRequest request : permanentData.unbanRequests.values()) {
            if(request.byDiscordUUID.equals(discordUUID)) return true;
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
    Map<String, String> GetVariables(Player.Punishment punishment) {
        Map<String, String> variables = new HashMap<>();
        variables.put("PUNISHMENT_NAME", punishment.punishmentName);
        variables.put("PUNISHMENT", punishment.punishment.toString());
        variables.put("PUNISHMENT_UNTIL", punishment.until.toString());
        variables.put("PUNISHMENT_UNTIL_SECONDS", Integer.toString(punishment.until.getSecond()));
        variables.put("PUNISHMENT_UNTIL_MINUTES", Integer.toString(punishment.until.getMinute()));
        variables.put("PUNISHMENT_UNTIL_HOURS", Integer.toString(punishment.until.getHour()));
        variables.put("PUNISHMENT_UNTIL_DAY", Integer.toString(punishment.until.getDayOfMonth()));
        variables.put("PUNISHMENT_UNTIL_MONTH", Integer.toString(punishment.until.getMonthValue()));
        variables.put("PUNISHMENT_UNTIL_MONTH_FULL", punishment.until.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()));
        variables.put("PUNISHMENT_UNTIL_YEAR", Integer.toString(punishment.until.getYear()));
        return variables;
    }
    Map<String, String> GetVariables(DataStorage.UnbanRequest request) {
        Map<String, String> variables = GetVariables(request.player);

        User requester = discord.GetUserByID(request.byDiscordUUID);
        variables.put("REQUESTER_NAME", requester.getName());
        variables.put("REQUESTER_GLOBAL_NAME", requester.getGlobalName());
        variables.put("REQUESTER_EFFECTIVE_NAME", requester.getEffectiveName());
        variables.put("REQUESTER_UUID", requester.getId());

        variables.put("ACCEPT_VOTES", String.valueOf(request.upVotes.size()));
        variables.put("DENY_VOTES", String.valueOf(request.downVotes.size()));
        variables.put("MIN_ACCEPT_VOTES", String.valueOf(voting.NeededUpVotes()));
        variables.put("MIN_DENY_VOTES", String.valueOf(voting.NeededDownvote()));

        variables.put("REASON", request.reason);

        Player.Punishment punishment = request.forPunishment;
        variables.putAll(GetVariables(punishment));

        return variables;
    }

    /***********************************************************************
     * Events
     ***********************************************************************/

    public void OnServerStart() {
        if(!config.enabled) return;

        request.SendRequestEmbed();
        voting.SendVotingMessages();
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if(!config.enabled) return;

        if(event.getComponentId().equals("unban-request-player-selector")) {
            request.OnPlayerPickStringSelect(event);
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if(!config.enabled) return;

        if (event.getComponentId().equals("unban-request-button")) {
            request.OnRequest(event);
        }
        else if (event.getComponentId().equals("unban-request-confirm-cancel")) {
            request.OnCancel(event);
        }
        else if (event.getComponentId().equals("unban-request-confirm-button")) {
            request.OnConfirmed(event);
        }
        else if(event.getComponentId().equals("unban-vote-up")) {
            voting.OnUpVote(event);
        }
        else if(event.getComponentId().equals("unban-vote-down")) {
            voting.OnDownVote(event);
        }
    }
    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if(!config.enabled) return;

        if (event.getModalId().equals("unban-request")) {
            request.OnModalFinish(event);
        }
    }

}