package org.hidde2727.DiscordPlugin.Flow.Voting;

import org.hidde2727.DiscordPlugin.Flow.Node;
import org.hidde2727.DiscordPlugin.Models.VotingRequest;

public class Properties {
    Config config;

    Node afterVotingNode;
    /**
     * Optional node.
     * Will make voting into rounds, where the selected result can be accepted or denied. This node should do the selection.
     * This node should, once a value has been selected proceed to the voting start node.
     */
    Node valueSelectorNode = null;

    /// Should be set to true to make the first round use the {prefix}-first key instead of the prefix for the voting message
    boolean isFirstRound = true;

    VotingRequest request;

    /** Language namespace */
    String namespace;
    /** Language key subscript */
    String keySubscript;
}