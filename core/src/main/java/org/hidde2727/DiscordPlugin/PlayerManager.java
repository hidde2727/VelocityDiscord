package org.hidde2727.DiscordPlugin;

import java.util.UUID;

import org.hidde2727.DiscordPlugin.DataStorage.Player;

public class PlayerManager {
    DataStorage storage;
    Config config;
    PlayerManager(Config config, DataStorage storage, boolean isServerOnlineMode) {
        this.storage = storage;
        this.config = config;

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

        if(config.useUUID && !isServerOnlineMode) {
            Logs.error("Server can't be in offline mode while config.useUUID is set to true. Disabling this plugin");
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
}
