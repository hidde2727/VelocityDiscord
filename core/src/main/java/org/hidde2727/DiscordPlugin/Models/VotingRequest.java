package org.hidde2727.DiscordPlugin.Models;

import org.hidde2727.DiscordPlugin.Discord.Embed;
import org.hidde2727.DiscordPlugin.VariableProvider;

import java.util.ArrayList;
import java.util.List;

public abstract class VotingRequest implements VariableProvider {
    public void addUpVote(String discordUUID) {
        upVotes.remove(discordUUID);
        downVotes.remove(discordUUID);
        upVotes.add(discordUUID);
    }
    public void addDownVote(String discordUUID) {
        upVotes.remove(discordUUID);
        downVotes.remove(discordUUID);
        downVotes.add(discordUUID);
    }
    public void resetVotes() {
        upVotes.clear();
        downVotes.clear();
    }
    public int getUpVotes() {
        return upVotes.size();
    }
    public int getDownVotes() {
        return downVotes.size();
    }

    public Embed getMessage() {
        return currentMessage;
    }
    public void setMessage(Embed embed) {
        currentMessage = embed;
    }

    List<String> upVotes = new ArrayList<>();
    List<String> downVotes = new ArrayList<>();
    Embed currentMessage;
}
