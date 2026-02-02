package org.hidde2727.VelocityDiscordPlugin.Discord;

import org.hidde2727.VelocityDiscordPlugin.StringProcessor;

import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent;

public interface ActionRowItem {
    public ActionRowChildComponent Build(StringProcessor processor, String namespace, int maxSearchDepth);
}
