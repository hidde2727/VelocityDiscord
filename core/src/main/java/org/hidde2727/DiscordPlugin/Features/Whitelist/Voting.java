package org.hidde2727.DiscordPlugin.Features.Whitelist;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.hidde2727.DiscordPlugin.Discord.ActionRow;
import org.hidde2727.DiscordPlugin.Discord.Button;
import org.hidde2727.DiscordPlugin.Discord.Discord;
import org.hidde2727.DiscordPlugin.Discord.Embed;
import org.hidde2727.DiscordPlugin.Logs;
import org.hidde2727.DiscordPlugin.Storage.Config;
import org.hidde2727.DiscordPlugin.Storage.DataStorage;

public class Voting {
    private Whitelist whitelist;
    private Discord discord;
    private Config.Whitelist.Voting config;
    private DataStorage permanentData;

    Voting(Whitelist whitelist) {
        this.whitelist = whitelist;
        this.discord = whitelist.discord;
        this.config = whitelist.config.voting;
        this.permanentData = whitelist.permanentData;

        if(config.enabled && !discord.DoesTextChannelExist(config.channel)) {
            Logs.error("Whitelist voting channel does not exist");
            this.config.enabled = false;
        } else if(config.enabled && !discord.CanBotAccesTextChannel(config.channel)) {
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
    void SendVotingMessages() {
        // Send the whitelist voting messages:
        for(DataStorage.WhitelistRequest request : permanentData.whitelistRequests.values()) {
            GetVotingMessage(request).SendInChannel(config.channel);
        }
    }


    int NeededUpVotes() {
        String configStr = config.acceptVotes;
        if(configStr.endsWith("%")) {
            // It is a percentage:
            int percentage = Integer.parseInt(configStr.substring(0, configStr.length() - 1));
            if(percentage == 0) return 1;
            double votingMembers = 0;
            if(!config.checkRoles) {
                votingMembers = discord.GetUsersInChannel(config.channel).size();
            } else {
                for(Member user : discord.GetUsersInChannel(config.channel)) {
                    for(Role role : user.getRoles()) {
                        if(config.allowedRoles.contains(role.getName())) {
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
        String configStr = config.denyVotes;
        if(configStr.endsWith("%")) {
            // It is a percentage:
            int percentage = Integer.parseInt(configStr.substring(0, configStr.length() - 1));
            if(percentage == 0) return 1;
            double votingMembers = 0;
            if(!config.checkRoles) {
                votingMembers = discord.GetUsersInChannel(config.channel).size();
            } else {
                for(Member user : discord.GetUsersInChannel(config.channel)) {
                    for(Role role : user.getRoles()) {
                        if(config.allowedRoles.contains(role.getName())) {
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
    boolean CheckWhitelistRequest(DataStorage.WhitelistRequest request) {
        if(request.upVotes.size() >= NeededUpVotes()) {
            // Accept the request
            OnAccept(request);
            whitelist.AcceptWhitelist(request);
            return true;
        } else if(request.downVotes.size() >= NeededDownvote()) {
            // Deny the request
            OnDeny(request);
            whitelist.DenyWhitelist(request);
            return true;
        }
        return false;
    }
    Embed GetVotingMessage(DataStorage.WhitelistRequest request) {
        return discord.CreateEmbed()
                .SetLanguageNamespace("whitelist", "voting")
                .SetVariables(whitelist.GetVariables(request))
                .AddActionRow(new ActionRow(
                        Button.Primary("whitelist-vote-up", "accept"),
                        Button.Destructive("whitelist-vote-down", "deny")
                ))
                .DeleteOnShutdown()
                .OnSend((String channelID, Long messageID) -> {
                    request.channelID = channelID;
                    request.messageID = messageID;
                });
    }
    void SendVotingNotAllowedEmbed(ButtonInteractionEvent event) {
        discord.CreateEmbed()
                .SetLanguageNamespace("whitelist", "votingNotAllowed")
                .SetVariables(whitelist.GetVariables(event.getUser()))
                .Send(event, true);
    }


    // 1. Either instant whitelist or get the request approved by the admins:
    void OnRequest(DataStorage.WhitelistRequest request) {
        // Create a voting request
        if(!config.enabled) {
            // Instant whitelist, as voting by admins for whitelisting isn't enabled
            whitelist.AcceptWhitelist(request);
            return;
        }
        GetVotingMessage(request).SendInChannel(config.channel);
    }
    // 2,1 A upvote
    void OnUpVote(ButtonInteractionEvent event) {
        // Check if the user has the permissions to vote:
        if(config.checkRoles && !discord.DoesUserHaveRoleInChannel(config.channel, event.getUser().getIdLong(), config.allowedRoles)) {
            SendVotingNotAllowedEmbed(event);
            return;
        }
        // Find the request:
        DataStorage.WhitelistRequest request = null;
        for(DataStorage.WhitelistRequest candidate : permanentData.whitelistRequests.values()) {
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
    // 2.2 A downvote
    void OnDownVote(ButtonInteractionEvent event) {
        // Check if the user has the permissions to vote:
        if(config.checkRoles && !discord.DoesUserHaveRoleInChannel(config.channel, event.getUser().getIdLong(), config.allowedRoles)) {
            SendVotingNotAllowedEmbed(event);
            return;
        }
        // Find the request:
        DataStorage.WhitelistRequest request = null;
        for(DataStorage.WhitelistRequest candidate : permanentData.whitelistRequests.values()) {
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

    void OnAccept(DataStorage.WhitelistRequest request) {
        // Change the voting message:
        Message toBeModified = discord.GetMessage(request.channelID, request.messageID);
        discord.CreateEmbed()
                .SetLanguageNamespace("whitelist", "votingAccepted")
                .SetVariables(whitelist.GetVariables(request))
                .Modify(toBeModified);
        discord.KeepMessageOnShutdown(new Discord.MessageID(toBeModified.getChannelId(), toBeModified.getIdLong()));
    }
    void OnDeny(DataStorage.WhitelistRequest request) {
        Message toBeModified = discord.GetMessage(request.channelID, request.messageID);
        discord.CreateEmbed()
                .SetLanguageNamespace("whitelist", "votingDenied")
                .SetVariables(whitelist.GetVariables(request))
                .Modify(toBeModified);
        discord.KeepMessageOnShutdown(new Discord.MessageID(toBeModified.getChannelId(), toBeModified.getIdLong()));
    }
}
