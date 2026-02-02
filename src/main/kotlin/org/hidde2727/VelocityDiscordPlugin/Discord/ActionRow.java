package org.hidde2727.VelocityDiscordPlugin.Discord;

import java.util.ArrayList;
import java.util.List;

public class ActionRow {
    List<ActionRowItem> items;

    ActionRow(List<ActionRowItem> items) {
        this.items = items;
    }

    public ActionRow(ActionRowItem item) {
        this.items = new ArrayList<ActionRowItem>(item);
    }
}
