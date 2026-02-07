package org.hidde2727.DiscordPlugin.Discord;

import org.hidde2727.DiscordPlugin.StringProcessor;

import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;

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

    public String GetLabel(StringProcessor processor, String namespace, int maxSearchDepth) {
        return processor.GetString(localizationKey + ".label", namespace, maxSearchDepth);
    }
    public ActionRowChildComponent Build(StringProcessor processor, String namespace, int maxSearchDepth) {
        return net.dv8tion.jda.api.components.buttons.Button.of(
            style,
            id,
            processor.GetString(localizationKey + ".label", namespace, maxSearchDepth),
            processor.GetEmoji(localizationKey + ".emoji", namespace, maxSearchDepth)
        );
    }
}
