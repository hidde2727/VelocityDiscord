package org.hidde2727.VelocityDiscordPlugin.Features;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hidde2727.VelocityDiscordPlugin.Config;
import org.hidde2727.VelocityDiscordPlugin.DataStorage;
import org.hidde2727.VelocityDiscordPlugin.Logs;
import org.hidde2727.VelocityDiscordPlugin.DataStorage.Whitelist.Request;
import org.hidde2727.VelocityDiscordPlugin.Discord.ActionRow;
import org.hidde2727.VelocityDiscordPlugin.Discord.Button;
import org.hidde2727.VelocityDiscordPlugin.Discord.Discord;
import org.hidde2727.VelocityDiscordPlugin.Discord.Embed;
import org.hidde2727.VelocityDiscordPlugin.Discord.TextField;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class Whitelist extends ListenerAdapter {
    private Discord discord;
    private Config.Whitelist config;
    private DataStorage.Whitelist permanentData;

    private Map<String, String> awaitingConfirmation = new HashMap<>();// Discord user to minecraft username request

    public Whitelist(Discord discord, Config.Whitelist config, DataStorage.Whitelist permanentData) {
        this.discord = discord;
        this.config = config;
        this.permanentData = permanentData;
        
        if(!config.enabled) return;
        if(config.request.enabled && !discord.DoesTextChannelExist(config.request.channel)) {
            Logs.logger.error("Whitelist voting channel does not exist");
            this.config.enabled = false;
        } else if(config.request.enabled && !discord.CanBotAccesTextChannel(config.request.channel)) {
            Logs.logger.error("The bot cannot access the whitelist voting channel");
            this.config.enabled = false;
        }
        if(config.voting.enabled && !discord.DoesTextChannelExist(config.voting.channel)) {
            Logs.logger.error("Whitelist voting channel does not exist");
            this.config.enabled = false;
        } else if(config.voting.enabled && !discord.CanBotAccesTextChannel(config.voting.channel)) {
            Logs.logger.error("The bot cannot access the whitelist voting channel");
            this.config.enabled = false;
        }

        try {
            NeededUpVotes();
            NeededDownvote();
        } catch(Exception exc) {
            Logs.logger.warn("Illegal down/up vote config");
            this.config.enabled = false;
        }
    }

    boolean HasRequest(String discordUUID) {
        for(Request request : permanentData.requests.values()) {
            if(request.discordUUID.equals(discordUUID)) return true;
        }
        return false;
    }
    boolean IsWhitelisted(String discordUUID) {
        return permanentData.whitelisted.containsValue(discordUUID);
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

    // 1. Initial embed people can use to get whitelisted:
    public void SendWhitelistReuestEmbed() {
        if(!config.enabled) return;
        if(!config.request.enabled) return;
        discord.CreateEmbed()
            .SetLocalizationNamespace("embeds.whitelistRequest", 2)
            .AddActionRow(new ActionRow(Button.Primary("whitelist-request-button", "button")))
            .DeleteOnShutdown()
            .SendInChannel(config.request.channel);
    }
    // 2. Someone pressed the button of 1, send modal to get their username
    public void OnWhitelistRequest(ButtonInteractionEvent event) {
        if(HasRequest(event.getUser().getId()) || IsWhitelisted(event.getUser().getId())) {
            // The user already has a request
            discord.CreateEmbed()
                .SetLocalizationNamespace("embeds.alreadyWhitelisted", 2)
                .SetVariable("DISCORD_NAME", event.getUser().getName())
                .SetVariable("DISCORD_GLOBAL_NAME", event.getUser().getGlobalName())
                .SetVariable("DISCORD_EFFECTIVE_NAME", event.getUser().getEffectiveName())
                .SetVariable("DISCORD_UUID", event.getUser().getId())
                .Send(event, true);
            return;
        }
        if(config.request.checkRoles && !discord.DoesUserHaveRoleInChannel(config.voting.channel, event.getUser().getIdLong(), config.voting.allowedRoles)) {
            // The user is not allowed to make a request
            discord.CreateEmbed()
                .SetLocalizationNamespace("embeds.whitelistRequestNotAllowed", 2)
                .SetVariable("DISCORD_NAME", event.getUser().getName())
                .SetVariable("DISCORD_GLOBAL_NAME", event.getUser().getGlobalName())
                .SetVariable("DISCORD_EFFECTIVE_NAME", event.getUser().getEffectiveName())
                .SetVariable("DISCORD_UUID", event.getUser().getId())
                .Send(event, true);
            return;
        }
        discord.CreateModal("whitelist-request")
            .SetLocalizationNamespace("modals.whitelistRequest", 2)
            .SetVariable("DISCORD_NAME", event.getUser().getName())
            .SetVariable("DISCORD_GLOBAL_NAME", event.getUser().getGlobalName())
            .SetVariable("DISCORD_EFFECTIVE_NAME", event.getUser().getEffectiveName())
            .SetVariable("DISCORD_UUID", event.getUser().getId())
            .Add(TextField.Short("username", "username", 3, 16))
            .Send(event);

    }
    // 3. Let the user check if they entered the correct username:
    public void OnWhitelistRequestFinished(ModalInteractionEvent event) {
        String minecraftUsername = event.getValue("username").getAsString();
        awaitingConfirmation.put(event.getUser().getId(), minecraftUsername);
        discord.CreateEmbed()
            .SetLocalizationNamespace("embeds.whitelistConfirm", 2)
            .SetVariable("PLAYER_NAME", minecraftUsername)
            .SetVariable("DISCORD_NAME", event.getUser().getName())
            .SetVariable("DISCORD_GLOBAL_NAME", event.getUser().getGlobalName())
            .SetVariable("DISCORD_EFFECTIVE_NAME", event.getUser().getEffectiveName())
            .SetVariable("DISCORD_UUID", event.getUser().getId())
            .AddActionRow(new ActionRow(
                Button.Primary("whitelist-request-confirm-button", "confirm"), 
                Button.Destructive("whitelist-request-confirm-cancel", "cancel")
            ))
            .Send(event, true);
    }
    // The request was canceled
    public void OnWhitelistRequestCancel(ButtonInteractionEvent event) {
        // awaitingConfirmation.remove(event.getUser().getId());
        discord.CreateEmbed()
            .SetLocalizationNamespace("embeds.whitelistRequestCanceled", 2)
            .SetVariable("DISCORD_NAME", event.getUser().getName())
            .SetVariable("DISCORD_GLOBAL_NAME", event.getUser().getGlobalName())
            .SetVariable("DISCORD_EFFECTIVE_NAME", event.getUser().getEffectiveName())
            .SetVariable("DISCORD_UUID", event.getUser().getId())
            .Send(event, true);
    }
    // 4. Either instant whitelist or get the request approved by admins:
    public void OnWhitelistRequestConfirm(ButtonInteractionEvent event) {
        event.getMessage().delete().queue();

        if(HasRequest(event.getUser().getId()) || IsWhitelisted(event.getUser().getId())) return;
        if(!config.voting.enabled) {
            // Instant whitelist, as voting by admins for whitelisting isn't enabled
            WhitelistPlayer(
                awaitingConfirmation.get(event.getUser().getId()),
                event.getUser().getId()
            );
            return;
        }
        // Send a voting request
        String minecraftName = awaitingConfirmation.get(event.getUser().getId());
        permanentData.requests.put(minecraftName, 
            new Request(event.getUser().getId(), minecraftName)
        );
        discord.CreateEmbed()
            .SetLocalizationNamespace("embeds.whitelistRequestConfirmed", 2)
            .SetVariable("PLAYER_NAME", minecraftName)
            .SetVariable("DISCORD_NAME", event.getUser().getName())
            .SetVariable("DISCORD_GLOBAL_NAME", event.getUser().getGlobalName())
            .SetVariable("DISCORD_EFFECTIVE_NAME", event.getUser().getEffectiveName())
            .SetVariable("DISCORD_UUID", event.getUser().getId())
            .Send(event, true);
        GetVotingMessage(minecraftName).SendInChannel(config.voting.channel);
    }
    // 5.1 A upvote
    public void OnWhitelistUpVote(ButtonInteractionEvent event) {
        // Check if the user has the permissions to vote:
        if(config.voting.checkRoles && !discord.DoesUserHaveRoleInChannel(config.voting.channel, event.getUser().getIdLong(), config.voting.allowedRoles)) {
            SendVotingNotAllowedEmbed(event);
            return;
        }
        // Find the request:
        Request request = null;
        for(Request candidate : permanentData.requests.values()) {
            if(candidate.messageID == event.getMessageIdLong()) {
                request = candidate;
                break;
            }
        }
        if(request == null) {
            Logs.logger.warn("Received an up-vote for an unknown whitelist request");
            return;
        }
        request.downVotes.remove(event.getUser().getId());
        request.upVotes.remove(event.getUser().getId());
        request.upVotes.add(event.getUser().getId());

        event.deferEdit().queue();
        // Check if the vote succeeded
        if(CheckWhitelistRequest(request.minecraftName)) return;
        // Else modify the voting message
        GetVotingMessage(request.minecraftName).Modify(event.getMessage());
    }
    // 5.2 A downvote
    public void OnWhitelistDownVote(ButtonInteractionEvent event) {
        // Check if the user has the permissions to vote:
        if(config.voting.checkRoles && !discord.DoesUserHaveRoleInChannel(config.voting.channel, event.getUser().getIdLong(), config.voting.allowedRoles)) {
            SendVotingNotAllowedEmbed(event);
            return;
        }
        // Find the request:
        Request request = null;
        for(Request candidate : permanentData.requests.values()) {
            if(candidate.messageID == event.getMessageIdLong()) {
                request = candidate;
                break;
            }
        }
        if(request == null) {
            Logs.logger.warn("Received an up-vote for an unknown whitelist request");
            return;
        }
        request.downVotes.remove(event.getUser().getId());
        request.upVotes.remove(event.getUser().getId());
        request.downVotes.add(event.getUser().getId());
        // Check if the vote succeeded
        if(CheckWhitelistRequest(request.minecraftName)) return;
        // Else modify the voting message
        GetVotingMessage(request.minecraftName).Modify(event.getMessage());
        event.deferEdit().queue();
    }

    // Last step, whitelist and announce the whitelist
    public void WhitelistPlayer(String minecraftName, String discordUUID) {
        permanentData.whitelisted.put(minecraftName, discordUUID);
        Request request = permanentData.requests.get(minecraftName);
        permanentData.requests.remove(minecraftName);

        User user = discord.GetUserByID(discordUUID);
        // Change the voting message:
        Message toBeModified = discord.GetMessage(request.channelID, request.messageID);
        discord.CreateEmbed()
            .SetLocalizationNamespace("embeds.whitelistVotingAccepted", 2)
            .SetVariable("PLAYER_NAME", minecraftName)
            .SetVariable("DISCORD_NAME", user.getName())
            .SetVariable("DISCORD_GLOBAL_NAME", user.getGlobalName())
            .SetVariable("DISCORD_EFFECTIVE_NAME", user.getEffectiveName())
            .SetVariable("DISCORD_UUID", user.getId())
            .SetVariable("ACCEPT_VOTES", String.valueOf(request.upVotes.size()))
            .SetVariable("DENY_VOTES", String.valueOf(request.downVotes.size()))
            .SetVariable("MIN_ACCEPT_VOTES", String.valueOf(NeededUpVotes()))
            .SetVariable("MIN_DENY_VOTES", String.valueOf(NeededDownvote()))
            .Modify(toBeModified);
        discord.KeepMessageOnShutdown(new Discord.MessageID(toBeModified.getChannelId(), toBeModified.getIdLong()));

        // Send the public whitelisted message:
        if(!config.onAccept.enabled) return;
        discord.CreateEmbed()
            .SetLocalizationNamespace("embeds.publicWhitelistAccepted", 2)
            .SetVariable("PLAYER_NAME", minecraftName)
            .SetVariable("DISCORD_NAME", user.getName())
            .SetVariable("DISCORD_GLOBAL_NAME", user.getGlobalName())
            .SetVariable("DISCORD_EFFECTIVE_NAME", user.getEffectiveName())
            .SetVariable("DISCORD_UUID", user.getId())
            .SetVariable("ACCEPT_VOTES", String.valueOf(request.upVotes.size()))
            .SetVariable("DENY_VOTES", String.valueOf(request.downVotes.size()))
            .SetVariable("MIN_ACCEPT_VOTES", String.valueOf(NeededUpVotes()))
            .SetVariable("MIN_DENY_VOTES", String.valueOf(NeededDownvote()))
            .SendInChannel(config.onAccept.channel);
    }
    // Last step, deny the whitelist and annouce it
    public void DenyWhitelistPlayer(String minecraftName, String discordUUID) {
        Request request = permanentData.requests.get(minecraftName);
        permanentData.requests.remove(minecraftName);

        User user = discord.GetUserByID(discordUUID);
        // Change the voting message:
        Message toBeModified = discord.GetMessage(request.channelID, request.messageID);
        discord.CreateEmbed()
            .SetLocalizationNamespace("embeds.whitelistVotingDenied", 2)
            .SetVariable("PLAYER_NAME", minecraftName)
            .SetVariable("DISCORD_NAME", user.getName())
            .SetVariable("DISCORD_GLOBAL_NAME", user.getGlobalName())
            .SetVariable("DISCORD_EFFECTIVE_NAME", user.getEffectiveName())
            .SetVariable("DISCORD_UUID", user.getId())
            .SetVariable("ACCEPT_VOTES", String.valueOf(request.upVotes.size()))
            .SetVariable("DENY_VOTES", String.valueOf(request.downVotes.size()))
            .SetVariable("MIN_ACCEPT_VOTES", String.valueOf(NeededUpVotes()))
            .SetVariable("MIN_DENY_VOTES", String.valueOf(NeededDownvote()))
            .Modify(toBeModified);
        discord.KeepMessageOnShutdown(new Discord.MessageID(toBeModified.getChannelId(), toBeModified.getIdLong()));

        // Send the public whitelist deny message:
        if(!config.onDeny.enabled) return;
        discord.CreateEmbed()
            .SetLocalizationNamespace("embeds.publicWhitelistDenied", 2)
            .SetVariable("PLAYER_NAME", minecraftName)
            .SetVariable("DISCORD_NAME", user.getName())
            .SetVariable("DISCORD_GLOBAL_NAME", user.getGlobalName())
            .SetVariable("DISCORD_EFFECTIVE_NAME", user.getEffectiveName())
            .SetVariable("DISCORD_UUID", user.getId())
            .SetVariable("ACCEPT_VOTES", String.valueOf(request.upVotes.size()))
            .SetVariable("DENY_VOTES", String.valueOf(request.downVotes.size()))
            .SetVariable("MIN_ACCEPT_VOTES", String.valueOf(NeededUpVotes()))
            .SetVariable("MIN_DENY_VOTES", String.valueOf(NeededDownvote()))
            .SendInChannel(config.onDeny.channel);
    }

    public boolean CheckWhitelistRequest(String minecraftName) {
        Request request = permanentData.requests.get(minecraftName);
        if(request.upVotes.size() >= NeededUpVotes()) {
            // Accept the request
            WhitelistPlayer(request.minecraftName, request.discordUUID);
            return true;
        } else if(request.downVotes.size() >= NeededDownvote()) {
            // Deny the request
            DenyWhitelistPlayer(request.minecraftName, request.discordUUID);
            return true;
        }
        return false;
    }
    public Embed GetVotingMessage(String minecraftName) {
        Request request = permanentData.requests.get(minecraftName);
        User user = discord.GetUserByID(request.discordUUID);
        return discord.CreateEmbed()
            .SetLocalizationNamespace("embeds.whitelistVoting", 2)
            .SetVariable("PLAYER_NAME", request.minecraftName)
            .SetVariable("DISCORD_NAME", user.getName())
            .SetVariable("DISCORD_GLOBAL_NAME", user.getGlobalName())
            .SetVariable("DISCORD_EFFECTIVE_NAME", user.getEffectiveName())
            .SetVariable("DISCORD_UUID", user.getId())
            .SetVariable("ACCEPT_VOTES", String.valueOf(request.upVotes.size()))
            .SetVariable("DENY_VOTES", String.valueOf(request.downVotes.size()))
            .SetVariable("MIN_ACCEPT_VOTES", String.valueOf(NeededUpVotes()))
            .SetVariable("MIN_DENY_VOTES", String.valueOf(NeededDownvote()))
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

    public void SendVotingNotAllowedEmbed(ButtonInteractionEvent event) {
        discord.CreateEmbed()
            .SetLocalizationNamespace("embeds.whitelistVotingNotAllowed", 2)
            .SetVariable("DISCORD_NAME", event.getUser().getName())
            .SetVariable("DISCORD_GLOBAL_NAME", event.getUser().getGlobalName())
            .SetVariable("DISCORD_EFFECTIVE_NAME", event.getUser().getEffectiveName())
            .SetVariable("DISCORD_UUID", event.getUser().getId())
            .Send(event, true);
    }

    @Subscribe
    public void onInitialize(ProxyInitializeEvent event) {
        SendWhitelistReuestEmbed();
        // Send the whitelist voting messages:
        for(Request request : permanentData.requests.values()) {
            GetVotingMessage(request.minecraftName).SendInChannel(config.voting.channel);
        }
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
