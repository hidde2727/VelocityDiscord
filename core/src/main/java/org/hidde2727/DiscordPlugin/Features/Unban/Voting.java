package org.hidde2727.DiscordPlugin.Features.Unban;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.hidde2727.DiscordPlugin.Discord.ActionRow;
import org.hidde2727.DiscordPlugin.Discord.Button;
import org.hidde2727.DiscordPlugin.Discord.Discord;
import org.hidde2727.DiscordPlugin.Discord.Discord.MessageID;
import org.hidde2727.DiscordPlugin.Discord.Embed;
import org.hidde2727.DiscordPlugin.Logs;
import org.hidde2727.DiscordPlugin.Storage.Config;
import org.hidde2727.DiscordPlugin.Storage.DataStorage;

public class Voting {
    private final Unban unban;
    private final Discord discord;
    private final Config.Unban.Voting config;
    private final DataStorage permanentData;

    Voting(Unban unban) {
        this.unban = unban;
        this.discord = unban.discord;
        this.config = unban.config.voting;
        this.permanentData = unban.permanentData;

        if(!config.enabled) return;

        if(!discord.DoesTextChannelExist(config.channel)) {
            Logs.error("Unban voting channel does not exist");
            this.config.enabled = false;
        } else if(!discord.CanBotAccesTextChannel(config.channel)) {
            Logs.error("The bot cannot access the unban voting channel");
            this.config.enabled = false;
        }

        try {
            NeededUpVotes();
            NeededDownvote();
        } catch(Exception exc) {
            Logs.warn("Illegal down/up vote config (for unbanning)");
            this.config.enabled = false;
        }
    }
    void SendVotingMessages() {
        // Send the unban voting messages:
        for(DataStorage.UnbanRequest request : permanentData.unbanRequests.values()) {
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
    boolean CheckUnbanRequest(DataStorage.UnbanRequest request) {
        if(request.upVotes.size() >= NeededUpVotes()) {
            // Accept the request
            OnAccept(request);
            unban.AcceptUnban(request);
            return true;
        } else if(request.downVotes.size() >= NeededDownvote()) {
            // Deny the request
            OnDeny(request);
            unban.DenyUnban(request);
            return true;
        }
        return false;
    }
    Embed GetVotingMessage(DataStorage.UnbanRequest request) {
        return discord.CreateEmbed()
                .SetLanguageNamespace("unban", "voting")
                .SetVariables(unban.GetVariables(request))
                .AddActionRow(new ActionRow(
                        Button.Primary("unban-vote-up", "accept"),
                        Button.Destructive("unban-vote-down", "deny")
                ))
                .DeleteOnShutdown("unban." + request.byDiscordUUID)
                .OnSend((MessageID messageID) -> {
                    request.messageID = messageID;
                });
    }
    void SendVotingNotAllowedEmbed(ButtonInteractionEvent event) {
        discord.CreateEmbed()
                .SetLanguageNamespace("unban", "votingNotAllowed")
                .SetVariables(unban.GetVariables(event.getUser()))
                .Send(event, true);
    }


    // 1. Either instant unban or get the request approved by the admins:
    void OnRequest(DataStorage.UnbanRequest request) {
        // Create a voting request
        if(!config.enabled) {
            // Instant whitelist, as voting by admins for unban isn't enabled
            unban.AcceptUnban(request);
            return;
        }
        GetVotingMessage(request).SendInChannel(config.channel);
    }
    // 2,1 A upvote
    void OnUpVote(ButtonInteractionEvent event) {
        // Check if the user has the permissions to vote:
        if(config.checkRoles && !discord.DoesUserHaveRole(event.getUser(), config.allowedRoles)) {
            SendVotingNotAllowedEmbed(event);
            return;
        }
        // Find the request:
        DataStorage.UnbanRequest request = null;
        for(DataStorage.UnbanRequest candidate : permanentData.unbanRequests.values()) {
            if(candidate.messageID.messageId == event.getMessageIdLong()) {
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
        if(CheckUnbanRequest(request)) return;
        // Else modify the voting message
        GetVotingMessage(request).Modify(event.getMessage());
    }
    // 2.2 A downvote
    void OnDownVote(ButtonInteractionEvent event) {
        // Check if the user has the permissions to vote:
        if(config.checkRoles && !discord.DoesUserHaveRole(event.getUser(), config.allowedRoles)) {
            SendVotingNotAllowedEmbed(event);
            return;
        }
        // Find the request:
        DataStorage.UnbanRequest request = null;
        for(DataStorage.UnbanRequest candidate : permanentData.unbanRequests.values()) {
            if(candidate.messageID.messageId == event.getMessageIdLong()) {
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
        if(CheckUnbanRequest(request)) return;
        // Else modify the voting message
        GetVotingMessage(request).Modify(event.getMessage());
        event.deferEdit().queue();
    }

    void OnAccept(DataStorage.UnbanRequest request) {
        // Change the voting message:
        Message toBeModified = discord.GetMessage(request.messageID);
        discord.CreateEmbed()
                .SetLanguageNamespace("unban", "votingAccepted")
                .SetVariables(unban.GetVariables(request))
                .Modify(toBeModified);
        discord.KeepMessageOnShutdown(request.messageID);
    }
    void OnDeny(DataStorage.UnbanRequest request) {
        Message toBeModified = discord.GetMessage(request.messageID);
        discord.CreateEmbed()
                .SetLanguageNamespace("unban", "votingDenied")
                .SetVariables(unban.GetVariables(request))
                .Modify(toBeModified);
        discord.KeepMessageOnShutdown(request.messageID);
    }
}
