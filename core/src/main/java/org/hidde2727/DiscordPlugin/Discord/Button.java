package org.hidde2727.DiscordPlugin.Discord;

import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.hidde2727.DiscordPlugin.StringProcessor;

import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;

import java.util.Map;

public class Button implements ActionRowItem {
    String id;
    String localizationKey;
    ButtonStyle style;
    Button(String id, String localizationKey, ButtonStyle style) {
        this.id = id;
        this.style = style;
        this.localizationKey = localizationKey;
    }

    public static Button Primary(String id, String localizationKey) {
        return new Button(id, localizationKey, ButtonStyle.PRIMARY);
    }
    public static Button Success(String id, String localizationKey) {
        return new Button(id, localizationKey, ButtonStyle.SUCCESS);
    }
    public static Button Secondary(String id, String localizationKey) {
        return new Button(id, localizationKey, ButtonStyle.SECONDARY);
    }
    public static Button Destructive(String id, String localizationKey) {
        return new Button(id, localizationKey, ButtonStyle.DANGER);
    }
    public static Button Link(String url, String localizationKey) {
        return new Button(url, localizationKey, ButtonStyle.LINK);
    }

    public String GetLabel(StringProcessor processor, Map<String, String> translations) {
        String label = translations.get("actions." + localizationKey + ".label");
        if(label == null) label = "NO_LABEL_SPECIFIED";
        return processor.GetString(label);
    }
    public ActionRowChildComponent Build(StringProcessor processor, Map<String, String> translations) {
        String label = processor.GetString(translations.get("actions." + localizationKey + ".label"));
        Emoji emoji = processor.GetEmoji(translations.get("actions." + localizationKey + ".emoji"));
        if(label == null && emoji == null) label = "EMPTY_BUTTON";
        return net.dv8tion.jda.api.components.buttons.Button.of(style, id, label, emoji);
    }
}
