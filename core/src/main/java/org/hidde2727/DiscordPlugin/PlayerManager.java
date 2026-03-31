package org.hidde2727.DiscordPlugin;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hidde2727.DiscordPlugin.Storage.Config;
import org.hidde2727.DiscordPlugin.Storage.DataStorage;
import org.hidde2727.DiscordPlugin.Storage.DataStorage.Player;

public class PlayerManager {
    DiscordPlugin plugin;
    DataStorage storage;
    Config config;
    PlayerManager(DiscordPlugin plugin) {
        this.plugin = plugin;
        this.storage = plugin.dataStorage;
        this.config = plugin.config;

        if(storage.players.isEmpty() && storage.whitelistRequests.isEmpty()) {
            this.storage.minecraftUUIDKey = config.useUUID;
            this.storage.connectDiscord = config.connectAccounts;
        } else {
            // Check if the data corresponds to our config
            if(this.storage.minecraftUUIDKey != config.useUUID) {
                Logs.error("Config does not correspond to the stored data (config.useUUID != storage.minecraftUUIDKey), please delete your data.yml. Falling back to using the settings in the data file");
                config.useUUID = this.storage.minecraftUUIDKey;
            }
            if(this.storage.connectDiscord != config.connectAccounts) {
                Logs.error("Config does not correspond to the stored data (config.connectAccounts != storage.storeDiscordUUID), please delete your data.yml. Falling back to using the settings in the data file");
                config.connectAccounts = this.storage.connectDiscord;
            }
        }

        if(config.useUUID && !plugin.implementation.IsOnlineMode()) {
            Logs.error("Server can't be in offline mode while config.useUUID is set to true. Disabling this plugin");
            plugin.disabled = true;
        }
    }

    public boolean ConnectAccounts() {
        return config.connectAccounts;
    }
    public boolean UseMinecraftUUID() {
        return config.useUUID;
    }

    public String GetMinecraftKey(String username, String UUID) {
        if(config.useUUID) {
            return UUID;
        } else {
            return username;
        }
    }
    public String GetMinecraftKey(String username, UUID UUID) {
        return GetMinecraftKey(
                username,
                UUID.toString().replaceAll("\\D", "")
        );
    }

    public Player GetPlayer(String minecraftKey) {
        return storage.players.get(minecraftKey);
    }
    public Player GetPlayer(String username, String UUID) {
        return GetPlayer(GetMinecraftKey(username, UUID));
    }
    public Player GetPlayerByDiscord(String discordUUID) {
        if(!ConnectAccounts()) return null;
        for(Player player : storage.players.values()) {
            if(player.discordUUID.equals(discordUUID)) return player;
        }
        return null;
    }

    public void RemovePlayerRoleIfNoPunishments(Player player) {
        if(!player.punishments.isEmpty()) return;
        if(!config.banning.giveRoleOnBan) return;
        plugin.discord.RemoveUserRole(player.discordUUID, config.banning.bannedRoleID);
    }
    public void RecheckPunishments(Player player) {
        boolean removedPunishment = false;
        for(Player.Punishment punishment : player.punishments) {
            if(punishment.punishment == Config.Banning.PunishmentPicker.PunishmentType.PermBan) continue;
            if(punishment.punishment == Config.Banning.PunishmentPicker.PunishmentType.Kick) {
                if(punishment.until.isBefore(OffsetDateTime.MIN.plusSeconds(1))) {
                    player.punishments.remove(punishment);
                    removedPunishment = true;
                }
            }
        }
        if(!removedPunishment) return;
        RemovePlayerRoleIfNoPunishments(player);
    }
    public Player.Punishment GetPunishment(Player player, Config.Banning.PunishmentPicker.PunishmentType type) {
        RecheckPunishments(player);
        for(Player.Punishment punishment : player.punishments) {
            if(punishment.punishment == type) return punishment;
        }
        return null;
    }
    public boolean HasPunishment(Player player, Config.Banning.PunishmentPicker.PunishmentType type) {
        return GetPunishment(player, type) != null;
    }
}
