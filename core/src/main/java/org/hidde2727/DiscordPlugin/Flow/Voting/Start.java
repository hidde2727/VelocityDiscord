package org.hidde2727.DiscordPlugin.Flow.Voting;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import org.hidde2727.DiscordPlugin.Discord.ActionRow;
import org.hidde2727.DiscordPlugin.Discord.Button;
import org.hidde2727.DiscordPlugin.Discord.Discord;
import org.hidde2727.DiscordPlugin.Discord.EmbedInfo;
import org.hidde2727.DiscordPlugin.Flow.Node;

/**
 * The start of the voting.
 * Will send the initial message and continue until the request is denied/accepted.
 */
public class Start extends Node {

    Start(Properties properties) {
        this.properties = properties;
    }

    @Override
    public void onStart() {
        if(!properties.config.enabled) {
            proceed(properties.afterVotingNode);
            return;
        }
        sendMessage();
    }

    @Override
    public void beforeRestart() {
        properties.request.getMessage().disable();
    }
    @Override
    public void afterRestart() {
        properties.request.getMessage().enable();
        // Resubscribe to the message:
        listenForEmbed(properties.request.getMessage());
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        // Check if the person is allowed to vote:
        if(
                properties.config.checkRoles &&
                !Discord.getInstance().doesUserHaveRole(event.getUser(), properties.config.allowedRoles)
        ) {
            sendNotAllowedEmbed(event);
            return;
        }

        // Modify the votes:
        if(event.getComponentId().equals("accept")) {
            properties.request.addUpVote(event.getUser().getId());
        } else if(event.getComponentId().equals("deny")) {
            properties.request.addDownVote(event.getUser().getId());
        }

        // Check if the voting succeeded:
        if(properties.request.getUpVotes() >= getNecessaryVotes(properties.config.acceptVotes)) {
            // Voting succeeded :)
            proceed(new Success(properties));
            return;
        }
        if(properties.request.getDownVotes() >= getNecessaryVotes(properties.config.denyVotes)) {
            // Voting failed :(
            proceed(new Failed(properties));
            return;
        }

        sendMessage();
    }

    private int getAmountVotingMembers() {
        if(!properties.config.checkRoles) {
            return Discord.getInstance().getUsersInChannel(properties.config.channel).size();
        }

        int votingMembers = 0;
        for(Member user : Discord.getInstance().getUsersInChannel(properties.config.channel)) {
            for(Role role : user.getRoles()) {
                if(properties.config.allowedRoles.contains(role.getName())) {
                    votingMembers++;
                    break;
                }
            }
        }
        return votingMembers;
    }
    private int getNecessaryVotes(String string) {
        if(string.endsWith("%")) {
            // It is a percentage:
            int percentage = Integer.parseInt(string.substring(0, string.length() - 1));
            if(percentage == 0) return 1;
            int votingMembers = getAmountVotingMembers();
            if(votingMembers == 0) return 1;
            return (int)Math.ceil(votingMembers * (percentage/100.));
        }
        return Integer.parseInt(string);
    }
    private void sendMessage() {
        if(properties.request.getMessage() == null) {
            // Send the initial message:
            getMessage(properties.isFirstRound)
                    .sendInChannel(properties.config.channel)
                    .whenCompleteAsync((message, error) -> {
                        properties.request.setMessage(message);
                        listenForEmbed(message);
                    });
        } else {
            // Else update the already sent message:
            properties.request.getMessage().modify(
                    getMessage(properties.isFirstRound)
            );
        }
    }
    private EmbedInfo getMessage(boolean isFirst) {
        return (new EmbedInfo())
                .setLanguage(
                        properties.namespace,
                        properties.keySubscript + (isFirst ? "first" : "")
                )
                .setVariables(properties.request)
                .addActionRow(new ActionRow(
                        Button.Primary("accept", "accept"),
                        Button.Secondary("deny", "deny")
                ));
    }

    private void sendNotAllowedEmbed(IReplyCallback event) {
        (new EmbedInfo())
                .setLanguage(
                        properties.namespace,
                        properties.keySubscript + "not-allowed"
                )
                .setVariables(properties.request)
                .sendAsReply(event, true);
    }

    private final Properties properties;
}
