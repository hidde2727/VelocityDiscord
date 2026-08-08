package org.hidde2727.DiscordPlugin.Flow.Voting;

import org.hidde2727.DiscordPlugin.Discord.EmbedInfo;
import org.hidde2727.DiscordPlugin.Flow.Node;

public class Success extends Node {
    Success(Properties properties) {
        this.properties = properties;
    }

    @Override
    public void onStart() {
        properties.request.getMessage().modify(
                (new EmbedInfo())
                        .setLanguage(
                                properties.namespace,
                                properties.keySubscript + "success"
                        )
                        .setVariables(properties.request)
        );
        proceed(properties.afterVotingNode);
    }
    @Override
    public void onEnd() {
        // Cleanup
        properties.request.resetVotes();
        properties.request.setMessage(null);
    }

    Properties properties;
}
