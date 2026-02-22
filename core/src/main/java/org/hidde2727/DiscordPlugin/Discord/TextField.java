package org.hidde2727.DiscordPlugin.Discord;

import org.hidde2727.DiscordPlugin.StringProcessor;

import net.dv8tion.jda.api.components.textinput.TextInputStyle;

import java.util.Map;

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


    public String GetLabel(StringProcessor processor, Map<String, String> translations) {
        String label = translations.get("actions." + localizationKey + ".label");
        if(label == null) label = "NO_LABEL_SPECIFIED";
        return processor.GetString(label);
    }
    public net.dv8tion.jda.api.components.textinput.TextInput Build(StringProcessor processor, Map<String, String> translations) {
        return net.dv8tion.jda.api.components.textinput.TextInput.create(id, type)
            .setPlaceholder(processor.GetString(translations.get("actions." + localizationKey + ".placeholder")))
            .setValue(processor.GetString(translations.get("actions." + localizationKey + ".value")))
            .setMinLength(minLength)
            .setMaxLength(maxLength)
            .setRequired(true)
            .build();
    }
}
