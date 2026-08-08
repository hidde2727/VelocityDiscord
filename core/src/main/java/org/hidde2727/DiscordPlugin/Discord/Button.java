package org.hidde2727.DiscordPlugin.Discord;

import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.hidde2727.DiscordPlugin.Storage.Language;
import org.hidde2727.DiscordPlugin.StringProcessor;
import org.hidde2727.DiscordPlugin.Logs;

import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;

import java.util.Map;

public class Button implements ActionRowItem {
    String id;
    String translationKey;
    ButtonStyle style;
    boolean disabled = false;
    Button(String id, String translationKey, ButtonStyle style) {
        this.id = id;
        this.style = style;
    }

    public static Button Primary(String id, String translationKey) {
        return new Button(id, translationKey, ButtonStyle.PRIMARY);
    }
    public static Button Success(String id, String translationKey) {
        return new Button(id, translationKey, ButtonStyle.SUCCESS);
    }
    public static Button Secondary(String id, String translationKey) {
        return new Button(id, translationKey, ButtonStyle.SECONDARY);
    }
    public static Button Destructive(String id, String translationKey) {
        return new Button(id, translationKey, ButtonStyle.DANGER);
    }
    public static Button Link(String url, String translationKey) {
        return new Button(url, translationKey, ButtonStyle.LINK);
    }

    @Override
    public String getTranslationKey() {
        return translationKey;
    }
    @Override
    public ActionRowChildComponent build(StringProcessor processor, Language.Action translations) {
        String label = processor.getString(translations.label);
        Emoji emoji = processor.getEmoji(translations.emoji);
        if(label == null && emoji == null) label = "EMPTY_BUTTON";
        return net.dv8tion.jda.api.components.buttons.Button.of(style, id, label, emoji).withDisabled(disabled);
    }
}
