package org.hidde2727.DiscordPlugin.Discord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hidde2727.DiscordPlugin.StringProcessor;

import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent;
import net.dv8tion.jda.api.utils.messages.MessageCreateRequest;

public class ActionRow {
    List<ActionRowItem> items;

    public ActionRow(ActionRowItem... items) {
        this.items = Arrays.asList(items);
    }

    public ActionRow(ActionRowItem item) {
        this.items = new ArrayList<ActionRowItem>();
        this.items.add(item);
    }

    void AddTo(MessageCreateRequest<?> message, StringProcessor processor, String namespace, int maxSearchDepth) {
        message.addComponents(Build(processor, namespace, maxSearchDepth));
    }
    net.dv8tion.jda.api.components.actionrow.ActionRow Build(StringProcessor processor, String namespace, int maxSearchDepth) {
        List<ActionRowChildComponent> components = new ArrayList<>();
        for(ActionRowItem item : items) {
            components.add(item.Build(processor, namespace, maxSearchDepth));
        }
        return net.dv8tion.jda.api.components.actionrow.ActionRow.of(components);
    }
}
