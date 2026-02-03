package org.hidde2727.VelocityDiscordPlugin.Discord;

import org.hidde2727.VelocityDiscordPlugin.StringProcessor;

import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent;

public interface ActionRowItem {
    public String GetLabel(StringProcessor processor, String namespace, int maxSearchDepth);
    public ActionRowChildComponent Build(StringProcessor processor, String namespace, int maxSearchDepth);
}
