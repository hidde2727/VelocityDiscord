package org.hidde2727.DiscordPlugin.Features.Whitelist;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.hidde2727.DiscordPlugin.*;
import org.hidde2727.DiscordPlugin.Storage.Config;
import org.hidde2727.DiscordPlugin.Storage.DataStorage;
import org.hidde2727.DiscordPlugin.Storage.DataStorage.Player;
import org.hidde2727.DiscordPlugin.Storage.DataStorage.WhitelistRequest;
import org.hidde2727.DiscordPlugin.Discord.Discord;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class Whitelist extends ListenerAdapter {
    final Discord discord;
    final Config.Whitelist config;
    final DataStorage permanentData;
    final PlayerManager players;

    Request request;
    Voting voting;
    Result result;

    public Whitelist(DiscordPlugin plugin) {
        this.discord = plugin.discord;
        this.config = plugin.config.whitelist;
        this.permanentData = plugin.dataStorage;
        this.players = plugin.players;

        if(!config.enabled) return;
        if(config.giveRoleOnWhitelist && !players.ConnectAccounts()) {
            Logs.error("Cannot give a whitelisted player a role if connectAccounts=false, falling back to not giving a role on ban");
            config.giveRoleOnWhitelist = false;
        }

        this.request = new Request(this);
        this.voting = new Voting(this);
        this.result = new Result(this);
    }

    void AddRequest(String minecraftName, String minecraftUUID, String byDiscordUUID) {
        String minecraftKey = players.GetMinecraftKey(minecraftName, minecraftUUID);
        DataStorage.WhitelistRequest request = new DataStorage.WhitelistRequest(byDiscordUUID, minecraftName, minecraftUUID, minecraftKey);
        permanentData.whitelistRequests.put(minecraftKey, request);

        voting.OnRequest(request);
    }
    void AcceptWhitelist(DataStorage.WhitelistRequest request) {
        players.AddPlayer(new DataStorage.Player(request.discordUUID, request.minecraftName, request.minecraftUUID), true);
        permanentData.whitelistRequests.remove(request.key);

        if(config.giveRoleOnWhitelist) {
            if(!discord.GiveUserRole(request.discordUUID, config.whitelistedRoleID)) {
                Logs.warn("Cannot give a whitelisted player a role that does not exist");
            }
        }

        result.OnAccept(request);
    }
    void DenyWhitelist(DataStorage.WhitelistRequest request) {
        permanentData.whitelistRequests.remove(request.key);
        result.OnDeny(request);
    }

    /***********************************************************************
     * Util
     ***********************************************************************/
    CompletableFuture<String> GetMinecraftUUID(String minecraftName) {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(new URI("https://api.mojang.com/users/profiles/minecraft/" + minecraftName))
                    .build();
        } catch(Exception exc) {
            return CompletableFuture.failedFuture(exc);
        }

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync((response) -> {
                    if(response.statusCode() == 404) {
                        // Player name was not found
                        return null;
                    }
                    JSONObject json = new JSONObject(response.body());
                    return json.getString("id");
                });
    }
    boolean HasRequest(String discordUUID) {
        for(WhitelistRequest request : permanentData.whitelistRequests.values()) {
            if(request.discordUUID.equals(discordUUID)) return true;
        }
        return false;
    }
    boolean IsWhitelistedByDiscord(String discordUUID) {
        return players.IsWhitelistedByDiscordUUID(discordUUID);
    }
    Map<String, String> GetVariables(User user) {
        Map<String, String> variables = new HashMap<>();
        variables.put("DISCORD_NAME", user.getName());
        variables.put("DISCORD_GLOBAL_NAME", user.getGlobalName());
        variables.put("DISCORD_EFFECTIVE_NAME", user.getEffectiveName());
        variables.put("DISCORD_UUID", user.getId());
        return variables;
    }
    Map<String, String> GetVariables(WhitelistRequest request) {
        Map<String, String> variables = GetVariables(discord.GetUserByID(request.discordUUID));
        variables.put("PLAYER_NAME", request.minecraftName);
        variables.put("PLAYER_UUID", request.minecraftUUID);
        variables.put("PLAYER_KEY", request.key);
        variables.put("ACCEPT_VOTES", String.valueOf(request.upVotes.size()));
        variables.put("DENY_VOTES", String.valueOf(request.downVotes.size()));
        variables.put("MIN_ACCEPT_VOTES", String.valueOf(voting.NeededUpVotes()));
        variables.put("MIN_DENY_VOTES", String.valueOf(voting.NeededDownvote()));
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

    public boolean OnPlayerPreLogin(String playerName, String playerUUID) {
        if(!config.enabled) return true;

        String minecraftKey = players.GetMinecraftKey(playerName, playerUUID);
        Player player = players.GetPlayer(minecraftKey);
        return player != null && player.whitelisted;
    }
    
    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if(!config.enabled) return;

        if (event.getComponentId().equals("whitelist-request-button")) {
            request.OnRequest(event);
        }
        else if (event.getComponentId().equals("whitelist-request-confirm-cancel")) {
            request.OnCancel(event);
        }
        else if (event.getComponentId().equals("whitelist-request-confirm-button")) {
            request.OnConfirmed(event);
        }
        else if(event.getComponentId().equals("whitelist-vote-up")) {
            voting.OnUpVote(event);
        }
        else if(event.getComponentId().equals("whitelist-vote-down")) {
            voting.OnDownVote(event);
        }
    }
    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        if(!config.enabled) return;

        if (event.getModalId().equals("whitelist-request")) {
            request.OnModalFinish(event);
        }
    }

}