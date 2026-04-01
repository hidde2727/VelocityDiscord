package org.hidde2727.DiscordPlugin.Discord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.hidde2727.DiscordPlugin.StringProcessor;

import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent;
import net.dv8tion.jda.api.utils.messages.MessageCreateRequest;

public class ActionRow {
    List<ActionRowItem> items;

    public ActionRow(ActionRowItem... items) {
        this.items = Arrays.asList(items);
    }
    public ActionRow(List<ActionRowItem> items) {
        this.items = items;
    }

    public ActionRow(ActionRowItem item) {
        this.items = new ArrayList<ActionRowItem>();
        this.items.add(item);
    }

    void AddTo(MessageCreateRequest<?> message, StringProcessor processor, Map<String, String> translations) {
        message.addComponents(Build(processor, translations));
    }
    net.dv8tion.jda.api.components.actionrow.ActionRow Build(StringProcessor processor, Map<String, String> translations) {
        List<ActionRowChildComponent> components = new ArrayList<>();
        for(ActionRowItem item : items) {
            components.add(item.Build(processor, translations));
        }
        return net.dv8tion.jda.api.components.actionrow.ActionRow.of(components);
    }
}
