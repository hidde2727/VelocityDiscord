package org.hidde2727.DiscordPlugin.Models;

import org.hidde2727.DiscordPlugin.Storage.Config;
import org.hidde2727.DiscordPlugin.VariableProvider;
import org.hidde2727.DiscordPlugin.VariableMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Player implements VariableProvider {

    Player(DiscordUser user, String minecraftName, String minecraftUUID) {
        this.discord = user;
        user.setPlayer(this);
        this.minecraftName = minecraftName;
        this.minecraftUUID = minecraftUUID;
    }

    public boolean whitelisted = false;
    public List<Punishment> punishments = new ArrayList<>();
    public DiscordUser discord;
    public String minecraftName;
    public String minecraftUUID;

    @Override
    public VariableMap getVariables() {
        VariableMap map = discord.getVariables();
        map.add("PLAYER_NAME", minecraftName);
        map.add("PLAYER_UUID", minecraftUUID);
        map.add("PLAYER_KEY", Config.getInstance().useUUID ? minecraftUUID : minecraftName);
        return map;
    }
}
