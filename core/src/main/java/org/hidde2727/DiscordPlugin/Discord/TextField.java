package org.hidde2727.DiscordPlugin.Discord;

import org.hidde2727.DiscordPlugin.StringProcessor;

import net.dv8tion.jda.api.components.textinput.TextInputStyle;

public class TextField {
    String id;
    String localizationKey;
    TextInputStyle type;
    int minLength;
    int maxLength;

    TextField(String id, String localizationKey, TextInputStyle type, int minLength, int maxLength) {
        this.id = id;
        this.localizationKey = localizationKey;
        this.type = type;
        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    public static TextField Short(String id, String localizationKey, int minLength, int maxLength) {
        return new TextField(id, localizationKey, TextInputStyle.SHORT, minLength, maxLength);
    }
    public static TextField Paragraph(String id, String localizationKey, int minLength, int maxLength) {
        return new TextField(id, localizationKey, TextInputStyle.PARAGRAPH, minLength, maxLength);        
    }


    public String GetLabel(StringProcessor processor, String namespace, int maxSearchDepth) {
        return processor.GetString(localizationKey + ".label", namespace, maxSearchDepth);
    }
    public net.dv8tion.jda.api.components.textinput.TextInput Build(StringProcessor processor, String namespace, int maxSearchDepth) {
        return net.dv8tion.jda.api.components.textinput.TextInput.create(id, type)
            .setPlaceholder(processor.GetString(localizationKey + ".placeholder", namespace, maxSearchDepth))
            .setMinLength(minLength)
            .setMaxLength(maxLength)
            .setRequired(true)
            .build();
    }
}
