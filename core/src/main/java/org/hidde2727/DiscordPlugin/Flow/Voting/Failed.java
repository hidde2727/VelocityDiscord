package org.hidde2727.DiscordPlugin.Flow.Voting;

import org.hidde2727.DiscordPlugin.Discord.EmbedInfo;
import org.hidde2727.DiscordPlugin.Flow.Node;

public class Failed extends Node {
    Failed(Properties properties) {
        this.properties = properties;
    }

    @Override
    public void onStart() {
        if(properties.valueSelectorNode != null) {
            // Time for selecting a new value:
            proceed(properties.valueSelectorNode);
            return;
        }
        properties.request.getMessage().modify(
                (new EmbedInfo())
                        .setLanguage(
                                properties.namespace,
                                properties.keySubscript + "failed"
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
