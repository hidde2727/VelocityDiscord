package org.hidde2727.DiscordPlugin.Models;

import net.dv8tion.jda.api.entities.User;
import org.hidde2727.DiscordPlugin.VariableProvider;
import org.hidde2727.DiscordPlugin.VariableMap;
import org.jetbrains.annotations.Nullable;

public class DiscordUser implements VariableProvider {
    public DiscordUser(User user, @Nullable Player player) {
        this.uuid = user.getId();
        this.name = user.getName();
        this.globalName = user.getGlobalName();
        this.effectiveName = user.getEffectiveName();
        this.player = player;
    }

    /** Should only be set by the Player class */
    void setPlayer(@Nullable Player player) {
        this.player = player;
    }
    @Nullable
    public Player getPlayer() {
        return this.player;
    }

    public String getUUID() {
        return uuid;
    }

    private String uuid;
    private String name;
    private String globalName;
    private String effectiveName;
    @Nullable
    private Player player = null;

    @Override
    public VariableMap getVariables() {
        VariableMap map = new VariableMap();
        map.add("DISCORD_NAME", name);
        map.add("DISCORD_GLOBAL_NAME", globalName);
        map.add("DISCORD_EFFECTIVE_NAME", effectiveName);
        map.add("DISCORD_UUID", uuid);
        return map;
    }
}
