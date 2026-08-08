package org.hidde2727.DiscordPlugin.Discord;

import net.dv8tion.jda.api.components.label.LabelChildComponent;
import org.hidde2727.DiscordPlugin.Storage.Language;
import org.hidde2727.DiscordPlugin.StringProcessor;

public interface ModalItem {
    public String getTranslationKey();
    public String getLabel(StringProcessor processor, Language.Action language);
    public LabelChildComponent build(StringProcessor processor, Language.Action language);
}
