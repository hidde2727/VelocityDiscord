package org.hidde2727.DiscordPlugin.Flow;

import org.hidde2727.DiscordPlugin.Discord.EventListener;

public class Node extends EventListener {

    public void onStart() {}
    public void onEnd() {}

    public void beforeRestart() {}
    public void afterRestart() {}

    /** Proceed to a next node: */
    public void proceed(Node node) {
        manager.proceedNode(this, node);
    }
    /** Ends this node and prevents further processing */
    public void endProcessing() {
        manager.endNode(this);
    }

    private FlowManager manager;
}
