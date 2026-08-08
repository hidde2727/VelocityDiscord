package org.hidde2727.DiscordPlugin.Flow;

import java.util.HashSet;
import java.util.Set;

public abstract class FlowManager extends Node {

    public FlowManager() {
        activeRequests.add(this);
        this.onStart();
    }
    public FlowManager(Set<Node> nodes) {
        activeRequests = nodes;
        // Here for the serialization, as this does not stay connected to the FlowManager:
        activeRequests.add(this);
        this.onStart();
    }

    @Override
    public void beforeRestart() {
        // Here for the serialization, as this does not stay connected to the FlowManager:
        activeRequests.remove(this);
    }

    void startNode(Node node) {
        activeRequests.add(node);
        node.onStart();
    }
    void proceedNode(Node oldNode, Node newNode) {
        oldNode.onEnd();
        activeRequests.remove(oldNode);
        activeRequests.add(newNode);
        newNode.onStart();
    }
    void endNode(Node node) {
        node.onEnd();
        activeRequests.remove(node);
    }

    private Set<Node> activeRequests = new HashSet<>();
}
