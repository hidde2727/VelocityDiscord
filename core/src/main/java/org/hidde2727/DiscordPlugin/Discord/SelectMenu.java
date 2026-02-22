package org.hidde2727.DiscordPlugin.Discord;

import java.util.Map;

import org.hidde2727.DiscordPlugin.StringProcessor;

import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu.SelectTarget;

public class SelectMenu implements ActionRowItem {
    enum Type {
        User,
        Channel,
        Role,
        Custom
    };
    String id;
    String localizationKey;
    Type type;
    Map<String, String> options;

    SelectMenu(String id, String localizationKey, Type type, Map<String, String> options) {
        this.id = id;
        this.type = type;
        this.options = options;
        this.localizationKey = localizationKey;
    }
    SelectMenu(String id, String localizationKey, Type type) {
        this(id, localizationKey, type, null);
    }

    public static SelectMenu Users(String id, String localizationKey) {
        return new SelectMenu(id, localizationKey, Type.User);
    }
    public static SelectMenu Channels(String id, String localizationKey) {
        return new SelectMenu(id, localizationKey, Type.Channel);

    }
    public static SelectMenu Roles(String id, String localizationKey) {
        return new SelectMenu(id, localizationKey, Type.Role);

    }
    public static SelectMenu Custom(String id, String localizationKey, Map<String, String> options) {
        return new SelectMenu(id, localizationKey, Type.Custom, options);

    }

    public String GetLabel(StringProcessor processor, Map<String, String> translations) {
        String label = translations.get("actions." + localizationKey + ".label");
        if(label == null) label = "NO_LABEL_SPECIFIED";
        return processor.GetString(label);
    }
    public ActionRowChildComponent Build(StringProcessor processor, Map<String, String> translations) {
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
                    entry.getKey(),
                    entry.getValue()
                );
            }
            return menu.setMaxValues(1).setMinValues(1).setRequired(true).build();
        }
        return null;
    }
}
