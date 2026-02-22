package org.hidde2727.DiscordPlugin.Features.Banning;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectInteraction;
import org.hidde2727.DiscordPlugin.Discord.*;
import org.hidde2727.DiscordPlugin.Logs;
import org.hidde2727.DiscordPlugin.Storage.Config;
import org.hidde2727.DiscordPlugin.Storage.DataStorage;

import java.util.stream.Collectors;

public class Punishment {
    private Banning banning;
    private Discord discord;
    private Config.Banning.PunishmentPicker config;
    private DataStorage permanentData;

    Punishment(Banning banning) {
        this.banning = banning;
        this.discord = banning.discord;
        this.config = banning.config.punishment;
        this.permanentData = banning.permanentData;

        if(config.enabled && !discord.DoesTextChannelExist(config.channel)) {
            Logs.error("Banning punishment channel does not exist");
            banning.config.enabled = false;
        } else if(config.enabled && !discord.CanBotAccesTextChannel(config.channel)) {
            Logs.error("The bot cannot access the banning punishment channel");
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
        for(DataStorage.BanRequest request : permanentData.banRequests.values()) {
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
            banning.DecidePunishment(request);
            return true;
        }
        if(request.downVotes.size() >= NeededDownvote()) {
            // Try again to get to a solution
            request.punishment = Config.Banning.PunishmentPicker.PunishmentType.Null;
            Message toBeModified = discord.GetMessage(request.channelID, request.messageID);
            GetVotingMessage(request).Modify(toBeModified);
            return true;
        }
        return false;
    }
    Embed GetVotingMessage(DataStorage.BanRequest request) {
        if(request.punishment == Config.Banning.PunishmentPicker.PunishmentType.Null) {
            // Send the punishment picker embed
            return discord.CreateEmbed()
                    .SetLanguageNamespace("banning", "punishmentPicker")
                    .SetVariables(banning.GetVariables(request, false))
                    .AddActionRow(new ActionRow(SelectMenu.Custom("ban-punishment-selector", "punishment",
                            config.punishments.keySet().stream().collect(Collectors.toMap(
                                    (v) -> v, (v) -> v
                            ))
                    )))
                    .DeleteOnShutdown()
                    .OnSend((String channelID, Long messageID) -> {
                        request.channelID = channelID;
                        request.messageID = messageID;
                    });
        }
        return discord.CreateEmbed()
                .SetLanguageNamespace("banning", "punishmentVoting")
                .SetVariables(banning.GetVariables(request, false))
                .AddActionRow(new ActionRow(
                        Button.Primary("ban-punishment-vote-up", "accept"),
                        Button.Destructive("ban-punishment-vote-down", "deny")
                ))
                .DeleteOnShutdown()
                .OnSend((String channelID, Long messageID) -> {
                    request.channelID = channelID;
                    request.messageID = messageID;
                });
    }
    void SendVotingNotAllowedEmbed(ButtonInteractionEvent event) {
        discord.CreateEmbed()
                .SetLanguageNamespace("banning", "punishmentVotingNotAllowed")
                .SetVariables(banning.GetVariables(event.getUser()))
                .Send(event, true);
    }


    // 1. Either instant whitelist or get the request approved by the admins:
    void OnRequest(DataStorage.BanRequest request) {
        if(!config.enabled) {
            // Instantaneously decide perm ban
            request.punishment = Config.Banning.PunishmentPicker.PunishmentType.PermBan;
            banning.DecidePunishment(request);
            return;
        }
        GetVotingMessage(request).SendInChannel(config.channel);
    }
    void OnPunishmentPick(StringSelectInteraction event) {
        // Find the request:
        DataStorage.BanRequest request = null;
        for(DataStorage.BanRequest candidate : permanentData.banRequests.values()) {
            if(candidate.messageID == event.getMessageIdLong()) {
                request = candidate;
                break;
            }
        }
        if(request == null) {
            Logs.warn("Received an up-vote for an unknown ban request");
            return;
        }
        String punishmentName = event.getValues().get(0);
        Config.Banning.PunishmentPicker.Punishment punishment = config.punishments.get(punishmentName);
        request.punishment = punishment.type;
        request.duration = Integer.parseInt(punishment.duration);
        request.adminDiscordUUID = event.getUser().getId();
        request.punishmentName = punishmentName;

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
        for(DataStorage.BanRequest candidate : permanentData.banRequests.values()) {
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
        for(DataStorage.BanRequest candidate : permanentData.banRequests.values()) {
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
        // Check if the vote succeeded
        if(CheckBanRequest(request)) return;
        // Else modify the voting message
        GetVotingMessage(request).Modify(event.getMessage());
        event.deferEdit().queue();
    }

    void OnDecided(DataStorage.BanRequest request) {
        // Change the voting message:
        Message toBeModified = discord.GetMessage(request.channelID, request.messageID);
        discord.CreateEmbed()
                .SetLanguageNamespace("banning", "punishmentDecided")
                .SetVariables(banning.GetVariables(request, false))
                .Modify(toBeModified);
        discord.KeepMessageOnShutdown(new Discord.MessageID(toBeModified.getChannelId(), toBeModified.getIdLong()));
    }
}