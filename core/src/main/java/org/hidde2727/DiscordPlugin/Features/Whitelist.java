package org.hidde2727.DiscordPlugin.Features;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.HashMap;
import java.util.Map;

import org.hidde2727.DiscordPlugin.*;
import org.hidde2727.DiscordPlugin.DataStorage.Player;
import org.hidde2727.DiscordPlugin.DataStorage.Request;
import org.hidde2727.DiscordPlugin.Discord.ActionRow;
import org.hidde2727.DiscordPlugin.Discord.Button;
import org.hidde2727.DiscordPlugin.Discord.Discord;
import org.hidde2727.DiscordPlugin.Discord.Embed;
import org.hidde2727.DiscordPlugin.Discord.TextField;
import org.json.JSONObject;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class Whitelist extends ListenerAdapter {
    private final Discord discord;
    private final Config.Whitelist config;
    private final DataStorage permanentData;
    private final PlayerManager players;

    private final Map<String, String> awaitingConfirmation = new HashMap<>();// Discord user to minecraft username request

    public Whitelist(DiscordPlugin plugin) {
        this.discord = plugin.discord;
        this.config = plugin.config.whitelist;
        this.permanentData = plugin.dataStorage;
        this.players = plugin.players;
        
        if(!config.enabled) return;
        if(config.request.enabled && !discord.DoesTextChannelExist(config.request.channel)) {
            Logs.error("Whitelist voting channel does not exist");
            this.config.enabled = false;
        } else if(config.request.enabled && !discord.CanBotAccesTextChannel(config.request.channel)) {
            Logs.error("The bot cannot access the whitelist voting channel");
            this.config.enabled = false;
        }
        if(config.voting.enabled && !discord.DoesTextChannelExist(config.voting.channel)) {
            Logs.error("Whitelist voting channel does not exist");
            this.config.enabled = false;
        } else if(config.voting.enabled && !discord.CanBotAccesTextChannel(config.voting.channel)) {
            Logs.error("The bot cannot access the whitelist voting channel");
            this.config.enabled = false;
        }

        try {
            NeededUpVotes();
            NeededDownvote();
        } catch(Exception exc) {
            Logs.warn("Illegal down/up vote config");
            this.config.enabled = false;
        }
    }

    /***********************************************************************
     * Util
     ***********************************************************************/

    boolean HasRequest(String discordUUID) {
        for(Request request : permanentData.whitelistRequests.values()) {
            if(request.discordUUID.equals(discordUUID)) return true;
        }
        return false;
    }
    boolean IsWhitelisted(String discordUUID) {
        for(Player player : permanentData.players.values()) {
            if(player.discordUUID.equals(discordUUID)) return player.whitelisted;
        }
        return false;
    }
    int NeededUpVotes() {
        String configStr = config.voting.acceptVotes;
        if(configStr.endsWith("%")) {
            // It is a percentage:
            int percentage = Integer.parseInt(configStr.substring(0, configStr.length() - 1));
            if(percentage == 0) return 1;
            double votingMembers = 0;
            if(!config.voting.checkRoles) {
                votingMembers = discord.GetUsersInChannel(config.voting.channel).size();
            } else {
                for(Member user : discord.GetUsersInChannel(config.voting.channel)) {
                    for(Role role : user.getRoles()) {
                        if(config.voting.allowedRoles.contains(role.getName())) {
                            votingMembers++;
                            break;
                        }
                    }
                }
            }

            if(votingMembers == 0) return 1;
            return (int)Math.ceil(votingMembers * (percentage/100.));
        }
        return Integer.parseInt(configStr);
    }
    int NeededDownvote() {
        String configStr = config.voting.denyVotes;
        if(configStr.endsWith("%")) {
            // It is a percentage:
            int percentage = Integer.parseInt(configStr.substring(0, configStr.length() - 1));
            if(percentage == 0) return 1;
            double votingMembers = 0;
            if(!config.voting.checkRoles) {
                votingMembers = discord.GetUsersInChannel(config.voting.channel).size();
            } else {
                for(Member user : discord.GetUsersInChannel(config.voting.channel)) {
                    for(Role role : user.getRoles()) {
                        if(config.voting.allowedRoles.contains(role.getName())) {
                            votingMembers++;
                            break;
                        }
                    }
                }
            }

            if(votingMembers == 0) return 1;
            return (int)Math.ceil(votingMembers * (percentage/100.));
        }
        return Integer.parseInt(configStr);
    }
    Map<String, String> GetVariables(User user) {
        Map<String, String> variables = new HashMap<>();
        variables.put("DISCORD_NAME", user.getName());
        variables.put("DISCORD_GLOBAL_NAME", user.getGlobalName());
        variables.put("DISCORD_EFFECTIVE_NAME", user.getEffectiveName());
        variables.put("DISCORD_UUID", user.getId());
        return variables;
    }
    Map<String, String> GetVariables(Request request) {
        Map<String, String> variables = GetVariables(discord.GetUserByID(request.discordUUID));
        variables.put("PLAYER_NAME", request.minecraftName);
        variables.put("PLAYER_UUID", request.minecraftUUID);
        variables.put("PLAYER_KEY", request.key);
        variables.put("ACCEPT_VOTES", String.valueOf(request.upVotes.size()));
        variables.put("DENY_VOTES", String.valueOf(request.downVotes.size()));
        variables.put("MIN_ACCEPT_VOTES", String.valueOf(NeededUpVotes()));
        variables.put("MIN_DENY_VOTES", String.valueOf(NeededDownvote()));
        return variables;
    }

    /***********************************************************************
     * General functions
     ***********************************************************************/

    boolean CheckWhitelistRequest(Request request) {
        if(request.upVotes.size() >= NeededUpVotes()) {
            // Accept the request
            WhitelistPlayer(request);
            return true;
        } else if(request.downVotes.size() >= NeededDownvote()) {
            // Deny the request
            DenyWhitelistPlayer(request);
            return true;
        }
        return false;
    }
    Embed GetVotingMessage(Request request) {
        return discord.CreateEmbed()
                .SetLocalizationNamespace("embeds.whitelistVoting", 2)
                .SetVariables(GetVariables(request))
                .AddActionRow(new ActionRow(
                        Button.Primary("whitelist-vote-up", "acceptButton"),
                        Button.Destructive("whitelist-vote-down", "denyButton")
                ))
                .DeleteOnShutdown()
                .OnSend((String channelID, Long messageID) -> {
                    request.channelID = channelID;
                    request.messageID = messageID;
                });
    }
    void SendVotingNotAllowedEmbed(ButtonInteractionEvent event) {
        discord.CreateEmbed()
                .SetLocalizationNamespace("embeds.whitelistVotingNotAllowed", 2)
                .SetVariables(GetVariables(event.getUser()))
                .Send(event, true);
    }

    /***********************************************************************
     * Whitelisting process
     ***********************************************************************/

    // 1. Initial embed people can use to get whitelisted:
    void SendWhitelistRequestEmbed() {
        if(!config.enabled) return;
        if(!config.request.enabled) return;
        discord.CreateEmbed()
            .SetLocalizationNamespace("embeds.whitelistRequest", 2)
            .AddActionRow(new ActionRow(Button.Primary("whitelist-request-button", "button")))
            .DeleteOnShutdown()
            .SendInChannel(config.request.channel);
    }
    // 2. Someone pressed the button of 1, send modal to get their username
    void OnWhitelistRequest(ButtonInteractionEvent event) {
        if(HasRequest(event.getUser().getId()) || (players.ConnectAccounts() && IsWhitelisted(event.getUser().getId()))) {
            // The user already has a request
            discord.CreateEmbed()
                .SetLocalizationNamespace("embeds.alreadyWhitelisted", 2)
                .SetVariables(GetVariables(event.getUser()))
                .Send(event, true);
            return;
        }
        if(config.request.checkRoles && !discord.DoesUserHaveRoleInChannel(config.voting.channel, event.getUser().getIdLong(), config.voting.allowedRoles)) {
            // The user is not allowed to make a request
            discord.CreateEmbed()
                .SetLocalizationNamespace("embeds.whitelistRequestNotAllowed", 2)
                .SetVariables(GetVariables(event.getUser()))
                .Send(event, true);
            return;
        }
        discord.CreateModal("whitelist-request")
            .SetLocalizationNamespace("modals.whitelistRequest", 2)
            .SetVariables(GetVariables(event.getUser()))
            .Add(TextField.Short("username", "username", 3, 16))
            .Send(event);

    }
    // 3. Let the user check if they entered the correct username:
    void OnWhitelistRequestFinished(ModalInteractionEvent event) {
        String minecraftUsername = event.getValue("username").getAsString();
        awaitingConfirmation.put(event.getUser().getId(), minecraftUsername);
        discord.CreateEmbed()
            .SetLocalizationNamespace("embeds.whitelistConfirm", 2)
            .SetVariable("PLAYER_NAME", minecraftUsername)
            .SetVariables(GetVariables(event.getUser()))
            .AddActionRow(new ActionRow(
                Button.Primary("whitelist-request-confirm-button", "confirm"), 
                Button.Destructive("whitelist-request-confirm-cancel", "cancel")
            ))
            .Send(event, true);
    }
    // The request was canceled
    void OnWhitelistRequestCancel(ButtonInteractionEvent event) {
        // awaitingConfirmation.remove(event.getUser().getId());
        discord.CreateEmbed()
            .SetLocalizationNamespace("embeds.whitelistRequestCanceled", 2)
            .SetVariables(GetVariables(event.getUser()))
            .Send(event, true);
    }
    // 4.a Either instant whitelist or get the request approved by admins:
    void OnWhitelistRequestConfirm(ButtonInteractionEvent event) {
        event.getMessage().delete().queue();

        if(HasRequest(event.getUser().getId()) || (players.ConnectAccounts() && IsWhitelisted(event.getUser().getId()))) return;
        String minecraftName = awaitingConfirmation.get(event.getUser().getId());
        if(players.UseMinecraftUUID()) {
            // Get the players UUID from Mojang:
            event.deferReply(true);

            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = null;
            try {
                request = HttpRequest.newBuilder()
                .uri(new URI("https://api.mojang.com/users/profiles/minecraft/" + minecraftName))
                .build();
            } catch(Exception exc) {
                Logs.warn(exc.getMessage());
                event.deferEdit();
                return;
            }

            httpClient.sendAsync(request, BodyHandlers.ofString())
            .whenCompleteAsync((response, error) -> {
                if(response.statusCode() == 404) {
                    // Player name was not found
                    discord.CreateEmbed()
                        .SetLocalizationNamespace("embeds.whitelistPlayerNotFound", 2)
                        .SetVariable("PLAYER_NAME", minecraftName)
                        .SetVariables(GetVariables(event.getUser()))
                        .Send(event, true);
                    return;
                }
                JSONObject json = new JSONObject(response.body());
                String minecraftUUID = json.getString("id");
                OnWhitelistRequestConfirm(event, minecraftUUID);
            });
        } else {
            OnWhitelistRequestConfirm(event, null);
        }
    }
    // 4.b Either instant whitelist or get the request approved by the admins:
    void OnWhitelistRequestConfirm(ButtonInteractionEvent event, String minecraftUUID) {
        String minecraftName = awaitingConfirmation.get(event.getUser().getId());
        // Create a voting request
        String minecraftKey = players.GetMinecraftKey(minecraftName, minecraftUUID);
        Request request = new Request(event.getUser().getId(), minecraftName, minecraftUUID, minecraftKey);
        permanentData.whitelistRequests.put(minecraftKey, request);
        if(!config.voting.enabled) {
            // Instant whitelist, as voting by admins for whitelisting isn't enabled
            WhitelistPlayer(request);
            return;
        }
        discord.CreateEmbed()
            .SetLocalizationNamespace("embeds.whitelistRequestConfirmed", 2)
            .SetVariables(GetVariables(request))
            .Send(event, true);
        GetVotingMessage(request).SendInChannel(config.voting.channel);
    }
    // 5.1 A upvote
    void OnWhitelistUpVote(ButtonInteractionEvent event) {
        // Check if the user has the permissions to vote:
        if(config.voting.checkRoles && !discord.DoesUserHaveRoleInChannel(config.voting.channel, event.getUser().getIdLong(), config.voting.allowedRoles)) {
            SendVotingNotAllowedEmbed(event);
            return;
        }
        // Find the request:
        Request request = null;
        for(Request candidate : permanentData.whitelistRequests.values()) {
            if(candidate.messageID == event.getMessageIdLong()) {
                request = candidate;
                break;
            }
        }
        if(request == null) {
            Logs.warn("Received an up-vote for an unknown whitelist request");
            return;
        }
        request.downVotes.remove(event.getUser().getId());
        request.upVotes.remove(event.getUser().getId());
        request.upVotes.add(event.getUser().getId());

        event.deferEdit().queue();
        // Check if the vote succeeded
        if(CheckWhitelistRequest(request)) return;
        // Else modify the voting message
        GetVotingMessage(request).Modify(event.getMessage());
    }
    // 5.2 A downvote
    void OnWhitelistDownVote(ButtonInteractionEvent event) {
        // Check if the user has the permissions to vote:
        if(config.voting.checkRoles && !discord.DoesUserHaveRoleInChannel(config.voting.channel, event.getUser().getIdLong(), config.voting.allowedRoles)) {
            SendVotingNotAllowedEmbed(event);
            return;
        }
        // Find the request:
        Request request = null;
        for(Request candidate : permanentData.whitelistRequests.values()) {
            if(candidate.messageID == event.getMessageIdLong()) {
                request = candidate;
                break;
            }
        }
        if(request == null) {
            Logs.warn("Received an up-vote for an unknown whitelist request");
            return;
        }
        request.downVotes.remove(event.getUser().getId());
        request.upVotes.remove(event.getUser().getId());
        request.downVotes.add(event.getUser().getId());
        // Check if the vote succeeded
        if(CheckWhitelistRequest(request)) return;
        // Else modify the voting message
        GetVotingMessage(request).Modify(event.getMessage());
        event.deferEdit().queue();
    }

    // Last step, whitelist and announce the whitelist
    void WhitelistPlayer(Request request) {
        // Add the player to the whitelist, remove the whitelist request
        permanentData.players.put(request.key, new Player(request.discordUUID, request.minecraftName, request.minecraftUUID));
        permanentData.players.get(request.key).whitelisted = true;
        permanentData.whitelistRequests.remove(request.key);

        if(config.giveRoleOnWhitelist) {
            if(!discord.GiveUserRole(request.discordUUID, config.whitelistedRoleID)) {
                Logs.warn("Cannot give a whitelisted player a role that does not exist");
            }
        }

        if(config.voting.enabled) {
            // Change the voting message:
            Message toBeModified = discord.GetMessage(request.channelID, request.messageID);
            discord.CreateEmbed()
                .SetLocalizationNamespace("embeds.whitelistVotingAccepted", 2)
                .SetVariables(GetVariables(request))
                .Modify(toBeModified);
            discord.KeepMessageOnShutdown(new Discord.MessageID(toBeModified.getChannelId(), toBeModified.getIdLong()));
        }

        // Send the public whitelisted message:
        if(!config.onAccept.enabled) return;
        Embed embed = discord.CreateEmbed()
            .SetLocalizationNamespace("embeds.publicWhitelistAccepted", 2)
            .SetVariables(GetVariables(request));
        embed.SendInChannel(config.onAccept.channel);
    }
    // Last step, deny the whitelist and announce it
    void DenyWhitelistPlayer(Request request) {
        permanentData.whitelistRequests.remove(request.key);

        // Change the voting message:
        Message toBeModified = discord.GetMessage(request.channelID, request.messageID);
        discord.CreateEmbed()
            .SetLocalizationNamespace("embeds.whitelistVotingDenied", 2)
            .SetVariables(GetVariables(request))
            .Modify(toBeModified);
        discord.KeepMessageOnShutdown(new Discord.MessageID(toBeModified.getChannelId(), toBeModified.getIdLong()));

        // Send the public whitelist deny message:
        if(!config.onDeny.enabled) return;
        discord.CreateEmbed()
            .SetLocalizationNamespace("embeds.publicWhitelistDenied", 2)
            .SetVariables(GetVariables(request))
            .SendInChannel(config.onDeny.channel);
    }

    /***********************************************************************
     * Events
     ***********************************************************************/

    public void OnServerStart() {
        SendWhitelistRequestEmbed();
        // Send the whitelist voting messages:
        for(Request request : permanentData.whitelistRequests.values()) {
            GetVotingMessage(request).SendInChannel(config.voting.channel);
        }
    }

    public boolean OnPlayerPreLogin(String playerName, String playerUUID) {
        String minecraftKey = players.GetMinecraftKey(playerName, playerUUID);
        Player player = players.GetPlayer(minecraftKey);
        return player != null && player.whitelisted;
    }
    
    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (event.getComponentId().equals("whitelist-request-button")) {
            OnWhitelistRequest(event);
        }
        else if (event.getComponentId().equals("whitelist-request-confirm-cancel")) {
            OnWhitelistRequestCancel(event);
        }
        else if (event.getComponentId().equals("whitelist-request-confirm-button")) {
            OnWhitelistRequestConfirm(event);
        }
        else if(event.getComponentId().equals("whitelist-vote-up")) {
            OnWhitelistUpVote(event);
        }
        else if(event.getComponentId().equals("whitelist-vote-down")) {
            OnWhitelistDownVote(event);
        }
    }
    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (event.getModalId().equals("whitelist-request")) {
            OnWhitelistRequestFinished(event);
        }
    }

}