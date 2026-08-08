package org.hidde2727.DiscordPlugin.Discord;

import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent;
import net.dv8tion.jda.api.components.label.LabelChildComponent;
import org.hidde2727.DiscordPlugin.Storage.Language;
import org.hidde2727.DiscordPlugin.StringProcessor;

import net.dv8tion.jda.api.components.textinput.TextInputStyle;

public class TextField implements ModalItem {
    String id;
    String translationKey;
    TextInputStyle type;
    int minLength;
    int maxLength;

    TextField(String id, String translationKey, TextInputStyle type, int minLength, int maxLength) {
        this.id = id;
        this.translationKey = translationKey;
        this.type = type;
        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    public static TextField Short(String id, String translationKey, int minLength, int maxLength) {
        return new TextField(id, translationKey, TextInputStyle.SHORT, minLength, maxLength);
    }
    public static TextField Paragraph(String id, String translationKey, int minLength, int maxLength) {
        return new TextField(id, translationKey, TextInputStyle.PARAGRAPH, minLength, maxLength);
    }

    @Override
    public String getTranslationKey() {
        return translationKey;
    }
    @Override
    public String getLabel(StringProcessor processor, Language.Action translations) {
        String label = translations.label;
        if(label == null) label = "NO_LABEL_SPECIFIED";
        return processor.getString(label);
    }
    @Override
    public LabelChildComponent build(StringProcessor processor, Language.Action translations) {
        return net.dv8tion.jda.api.components.textinput.TextInput.create(id, type)
            .setPlaceholder(processor.getString(translations.placeholder))
            .setValue(processor.getString(translations.value))
            .setMinLength(minLength)
            .setMaxLength(maxLength)
            .setRequired(true)
            .build();
    }
}
