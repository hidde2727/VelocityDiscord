package org.hidde2727.VelocityDiscordPlugin.Discord;

import java.util.Map;

import org.hidde2727.VelocityDiscordPlugin.StringProcessor;

import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu.SelectTarget;
import net.dv8tion.jda.api.entities.emoji.Emoji;

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
    Map<String, String> optionIds;

    SelectMenu(String id, String localizationKey, Type type, Map<String, String> optionIds) {
        this.id = id;
        this.type = type;
        this.optionIds = optionIds;
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
    public static SelectMenu Custom(String id, String localizationKey, Map<String, String> optionIds) {
        return new SelectMenu(id, localizationKey, Type.Custom, optionIds);

    }

    public String GetLabel(StringProcessor processor, String namespace, int maxSearchDepth) {
        return processor.GetString(localizationKey + ".label", namespace, maxSearchDepth);
    }
    public ActionRowChildComponent Build(StringProcessor processor, String namespace, int maxSearchDepth) {
        if(type == Type.User) {
            return EntitySelectMenu.create(id, SelectTarget.USER).build();
        } else if(type == Type.Channel) {
            return EntitySelectMenu.create(id, SelectTarget.CHANNEL).build();
        } else if(type == Type.Role) {
            return EntitySelectMenu.create(id, SelectTarget.ROLE).build();
        } else if(type == Type.Custom) {
            StringSelectMenu.Builder menu = StringSelectMenu.create(id);
            for(Map.Entry<String, String> entry : optionIds.entrySet()) {
                menu.addOption(
                    entry.getKey(),
                    processor.GetString(localizationKey + ".option." + entry.getValue() + ".label", namespace, maxSearchDepth),
                    processor.GetString(localizationKey + ".option." + entry.getValue() + ".description", namespace, maxSearchDepth),
                    Emoji.fromFormatted(processor.GetString(localizationKey + ".option." + entry.getValue() + ".emoji", namespace, maxSearchDepth))
                );
            }
            return menu.setRequired(true).build();
        }
        return null;
    }
}
