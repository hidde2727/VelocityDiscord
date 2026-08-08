package org.hidde2727.DiscordPlugin.Discord;

import org.hidde2727.DiscordPlugin.Storage.Language;
import org.hidde2727.DiscordPlugin.StringProcessor;

import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent;

import java.util.Map;

public interface ActionRowItem {
    public String getTranslationKey();
    public ActionRowChildComponent build(StringProcessor processor, Language.Action language);
}
