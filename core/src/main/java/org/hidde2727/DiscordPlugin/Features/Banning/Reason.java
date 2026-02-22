package org.hidde2727.DiscordPlugin.Features.Banning;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import org.hidde2727.DiscordPlugin.Discord.*;
import org.hidde2727.DiscordPlugin.Logs;
import org.hidde2727.DiscordPlugin.Storage.Config;
import org.hidde2727.DiscordPlugin.Storage.DataStorage;

public class Reason {
    private Banning banning;
    private Discord discord;
    private Config.Banning.ReasonPicker config;
    private DataStorage permanentData;

    Reason(Banning banning) {
        this.banning = banning;
        this.discord = banning.discord;
        this.config = banning.config.reason;
        this.permanentData = banning.permanentData;

        if(config.enabled && !discord.DoesTextChannelExist(config.channel)) {
            Logs.error("Banning reason channel does not exist");
            banning.config.enabled = false;
        } else if(config.enabled && !discord.CanBotAccesTextChannel(config.channel)) {
            Logs.error("The bot cannot access the banning reason channel");
            banning.config.enabled = false;
        }

        try {
            NeededUpVotes();
            NeededDownvote();
        } catch(Exception exc) {
            Logs.warn("Illegal down/up vote config");
            banning.config.enabled = false;
        }
    }
    void SendVotingMessages() {
        // Send the whitelist voting messages:
        for(DataStorage.BanRequest request : permanentData.banRequestsDecided.values()) {
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
    boolean CheckBanRequest(DataStorage.BanRequest request) {
        if(request.upVotes.size() >= NeededUpVotes()) {
            // Accept the request
            OnDecided(request);
            banning.OnDecideReason(request);
            return true;
        } else if(request.downVotes.size() >= NeededDownvote()) {
            // Try again
            request.reason = null;
            Message toBeModified = discord.GetMessage(request.channelID, request.messageID);
            GetVotingMessage(request).Modify(toBeModified);
            return true;
        }
        return false;
    }
    Embed GetVotingMessage(DataStorage.BanRequest request) {
        if(request.adminDiscordUUID == null && request.reason != null) {
            // First message, no changes made
            return discord.CreateEmbed()
                    .SetLanguageNamespace("banning", "reasonFirstVote")
                    .SetVariables(banning.GetVariables(request, true))
                    .AddActionRow(new ActionRow(
                            Button.Primary("ban-reason-vote-up", "accept"),
                            Button.Destructive("ban-reason-vote-down", "deny")
                    ))
                    .DeleteOnShutdown()
                    .OnSend((String channelID, Long messageID) -> {
                        request.channelID = channelID;
                        request.messageID = messageID;
                    });
        }
        else if(request.reason == null) {
            // Reason denied, let someone change the request
            return discord.CreateEmbed()
                    .SetLanguageNamespace("banning", "reasonPicker")
                    .SetVariables(banning.GetVariables(request, true))
                    .AddActionRow(new ActionRow(
                            Button.Primary("ban-reason-change", "change")
                    ))
                    .DeleteOnShutdown()
                    .OnSend((String channelID, Long messageID) -> {
                        request.channelID = channelID;
                        request.messageID = messageID;
                    });
        }
        return discord.CreateEmbed()
                .SetLanguageNamespace("banning", "reasonVoting")
                .SetVariables(banning.GetVariables(request, true))
                .AddActionRow(new ActionRow(
                        Button.Primary("ban-reason-vote-up", "accept"),
                        Button.Destructive("ban-reason-vote-down", "deny")
                ))
                .DeleteOnShutdown()
                .OnSend((String channelID, Long messageID) -> {
                    request.channelID = channelID;
                    request.messageID = messageID;
                });
    }
    void SendVotingNotAllowedEmbed(IReplyCallback event) {
        discord.CreateEmbed()
                .SetLanguageNamespace("banning", "reasonVotingNotALlowed")
                .SetVariables(banning.GetVariables(event.getUser()))
                .Send(event, true);
    }


    // 1. Either instant accept or get the reason approved by the admins:
    void OnRequest(DataStorage.BanRequest request) {
        if(!config.enabled) {
            // Instant decide, no voting/changes
            banning.OnDecideReason(request);
            return;
        }
        Message toBeModified = discord.GetMessage(request.channelID, request.messageID);
        GetVotingMessage(request).Modify(toBeModified);
    }
    void OnChangeButton(ButtonInteractionEvent event) {
        // Check if the user has the permissions to vote:
        if(config.checkRoles && !discord.DoesUserHaveRoleInChannel(config.channel, event.getUser().getIdLong(), config.allowedRoles)) {
            SendVotingNotAllowedEmbed(event);
            return;
        }
        // Find the request:
        DataStorage.BanRequest request = null;
        for(DataStorage.BanRequest candidate : permanentData.banRequestsDecided.values()) {
            if(candidate.messageID == event.getMessageIdLong()) {
                request = candidate;
                break;
            }
        }
        if(request == null) {
            Logs.warn("Received an change request for an unknown ban request");
            return;
        }

        discord.CreateModal("banning-reason-change-" + request.key)
                .SetLanguageNamespace("banning", "reasonChange")
                .SetVariables(banning.GetVariables(request, true))
                .Add(TextField.Short("reason", "reason", 1, 1024))
                .Send(event);
    }
    void OnReasonPick(ModalInteractionEvent event) {
        // Check if the user has the permissions to vote:
        if(config.checkRoles && !discord.DoesUserHaveRoleInChannel(config.channel, event.getUser().getIdLong(), config.allowedRoles)) {
            SendVotingNotAllowedEmbed(event);
            return;
        }
        // Find the request:
        String minecraftKey = event.getModalId().substring(22);
        DataStorage.BanRequest request = permanentData.banRequestsDecided.get(minecraftKey);
        if(request == null) {
            Logs.warn("Received an change request for an unknown ban request");
            return;
        }

        request.reason = event.getValue("reason").getAsString();
        request.adminDiscordUUID = event.getUser().getId();
        request.upVotes.clear();
        request.downVotes.clear();
        request.upVotes.add(event.getUser().getId());

        event.deferEdit().queue();
        // Check if the vote succeeded
        if(CheckBanRequest(request)) return;
        // Else modify the voting message
        GetVotingMessage(request).Modify(event.getMessage());
    }
    // 2,1 A upvote
    void OnUpVote(ButtonInteractionEvent event) {
        // Check if the user has the permissions to vote:
        if(config.checkRoles && !discord.DoesUserHaveRoleInChannel(config.channel, event.getUser().getIdLong(), config.allowedRoles)) {
            SendVotingNotAllowedEmbed(event);
            return;
        }
        // Find the request:
        DataStorage.BanRequest request = null;
        for(DataStorage.BanRequest candidate : permanentData.banRequestsDecided.values()) {
            if(candidate.messageID == event.getMessageIdLong()) {
                request = candidate;
                break;
            }
        }
        if(request == null) {
            Logs.warn("Received an up-vote for an unknown ban request");
            return;
        }
        request.downVotes.remove(event.getUser().getId());
        request.upVotes.remove(event.getUser().getId());
        request.upVotes.add(event.getUser().getId());

        event.deferEdit().queue();
        // Check if the vote succeeded
        if(CheckBanRequest(request)) return;
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
        DataStorage.BanRequest request = null;
        for(DataStorage.BanRequest candidate : permanentData.banRequestsDecided.values()) {
            if(candidate.messageID == event.getMessageIdLong()) {
                request = candidate;
                break;
            }
        }
        if(request == null) {
            Logs.warn("Received an up-vote for an unknown ban request");
            return;
        }
        request.downVotes.remove(event.getUser().getId());
        request.upVotes.remove(event.getUser().getId());
        request.downVotes.add(event.getUser().getId());

        event.deferEdit().queue();
        // Check if the vote succeeded
        if(CheckBanRequest(request)) return;
        // Else modify the voting message
        GetVotingMessage(request).Modify(event.getMessage());
    }

    void OnDecided(DataStorage.BanRequest request) {
        // Change the voting message:
        Message toBeModified = discord.GetMessage(request.channelID, request.messageID);
        discord.CreateEmbed()
                .SetLanguageNamespace("banning", "reasonDecided")
                .SetVariables(banning.GetVariables(request, true))
                .Modify(toBeModified);
        discord.KeepMessageOnShutdown(new Discord.MessageID(toBeModified.getChannelId(), toBeModified.getIdLong()));
    }
}