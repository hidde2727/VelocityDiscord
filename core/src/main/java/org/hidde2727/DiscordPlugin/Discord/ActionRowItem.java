package org.hidde2727.DiscordPlugin.Discord;

import org.hidde2727.DiscordPlugin.StringProcessor;

import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent;

import java.util.Map;

public interface ActionRowItem {
    public String GetLabel(StringProcessor processor, Map<String, String> translations);
    public ActionRowChildComponent Build(StringProcessor processor, Map<String, String> translations);
}
