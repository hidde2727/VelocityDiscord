package org.hidde2727.DiscordPlugin.Discord;

import java.util.Map;

import net.dv8tion.jda.api.components.label.LabelChildComponent;
import org.hidde2727.DiscordPlugin.Storage.Language;
import org.hidde2727.DiscordPlugin.StringProcessor;

import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu.SelectTarget;

public class SelectMenu implements ActionRowItem, ModalItem {
    enum Type {
        User,
        Channel,
        Role,
        Custom
    }
    String id;
    String translationKey;
    Type type;
    Map<String, String> options;

    SelectMenu(String id, String translationKey, Type type, Map<String, String> options) {
        this.id = id;
        this.translationKey = translationKey;
        this.type = type;
        this.options = options;
    }
    SelectMenu(String id, String translationKey, Type type) {
        this(id, translationKey, type, null);
    }

    public static SelectMenu Users(String id, String translationKey) {
        return new SelectMenu(id, translationKey, Type.User);
    }
    public static SelectMenu Channels(String id, String translationKey) {
        return new SelectMenu(id, translationKey, Type.Channel);

    }
    public static SelectMenu Roles(String id, String translationKey) {
        return new SelectMenu(id, translationKey, Type.Role);

    }
    public static SelectMenu Custom(String id, String translationKey, Map<String, String> options) {
        return new SelectMenu(id, translationKey, Type.Custom, options);

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
    public net.dv8tion.jda.api.components.selections.SelectMenu build(StringProcessor processor, Language.Action translations) {
        if(type == Type.User) {
            return EntitySelectMenu.create(id, SelectTarget.USER).setMaxValues(1).setMinValues(1).setRequired(true).build();
        } else if(type == Type.Channel) {
            return EntitySelectMenu.create(id, SelectTarget.CHANNEL).setMaxValues(1).setMinValues(1).setRequired(true).build();
        } else if(type == Type.Role) {
            return EntitySelectMenu.create(id, SelectTarget.ROLE).setMaxValues(1).setMinValues(1).setRequired(true).build();
        } else if(type == Type.Custom) {
            StringSelectMenu.Builder menu = StringSelectMenu.create(id);
            for(Map.Entry<String, String> entry : options.entrySet()) {
                menu.addOption(
                    entry.getValue(),
                    entry.getKey()
                );
            }
            return menu.setMaxValues(1).setMinValues(1).setRequired(true).build();
        }
        return null;
    }
}
