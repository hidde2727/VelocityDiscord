package org.hidde2727.DiscordPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.hidde2727.DiscordPlugin.Discord.Discord;
import org.hidde2727.DiscordPlugin.Features.*;
import org.hidde2727.DiscordPlugin.Models.Player;
import org.hidde2727.DiscordPlugin.Storage.Config;
import org.hidde2727.DiscordPlugin.Storage.DataStorage;
import org.hidde2727.DiscordPlugin.Storage.Language;

public class DiscordPlugin {
    private static final int AUTO_SAVE_INTERVAL = 300000; // Every 5 minutes

    public DiscordPlugin(Implementation implementation) {
        this.implementation = implementation;
        Logs.useForLogging = implementation;

        Path dataDirectory = implementation.getDataDirectory();

        CreateDirectoryIfNotExists(dataDirectory);
        File configFile = dataDirectory.resolve("config.yml").toFile();
        CreateFileIfNotExists(configFile, "config.yml");
        File messageFile = dataDirectory.resolve("language.yml").toFile();
        CreateFileIfNotExists(messageFile, "language.yml");
        File dataFile = dataDirectory.resolve("data.yml").toFile();

        Config.init(configFile);

        if(!DataStorage.init(dataFile)) {
            disabled = true;
            Logs.warn("Failed to load data storage file, disabling the plugin");
            return;
        }
        if(DataStorage.getInstance().isBackup) {
            Logs.warn("The data file loaded is marked as a backup, some data may have been lost since the previous time Discordio was run.");
            Logs.warn("Last save: " + DataStorage.getInstance().storedAt);
        }
        if(!Language.init(messageFile)) {
            disabled = true;
            Logs.warn("Failed to load language file, disabling the plugin");
            return;
        }

        try {
            Discord.init(Config.getInstance().botToken, Config.getInstance().guildId);
        } catch(Exception exc) {
            Logs.warn(exc.getMessage());
            disabled = true;
            return;
        }
    }

    public void onServerStart() {
        if(disabled) return;

        // Add all the features
        features.add(new OnJoin());
        features.add(new OnLeave());
        features.add(new OnMessage(implementation));
        features.add(new OnStart());
        features.add(new OnStop());

        for(Feature feature : features) {
            feature.onServerStart();
        }

        // Set up the auto save:
        DiscordPlugin plugin = this;
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Logs.info("Auto saving!");
                plugin.StoreToDisk(true);
            }
        }, 0, AUTO_SAVE_INTERVAL);
    }
    public void onServerStop() {
        Logs.info("Stopping the discord plugin");
        if(!disabled) {
            for(Feature feature : features) {
                feature.onServerStop();
            }
        }

        if(Discord.getInstance() != null) {
            Discord.shutdown();
        }

        if(disabled) return;
        disabled = true;

        StoreToDisk(false);
    }
    public void onPlayerMessage(String onServer, String playerName, String playerUUID, String message) {
        if(disabled) return;

        Player player = DataStorage.getInstance().getPlayer(playerName, playerUUID);
        for(Feature feature : features) {
            feature.onPlayerMessage(onServer, player, message);
        }
    }
    public boolean onPlayerPreLogin(String playerName, String playerUUID) {
        if(disabled) {
            Logs.warn("Login attempt by '" + playerName + "' with uuid '" + playerUUID + "' was denied, because discordio is in an error state");
            return false;
        }

        Player player = DataStorage.getInstance().getPlayer(playerName, playerUUID);
        for(Feature feature : features) {
            if(!feature.onPlayerPreLogin(player)) {
                return false;
            }
        }
        return true;
    }
    public void onPlayerConnect(String playerName, String playerUUID) {
        if(disabled) return;

        Player player = DataStorage.getInstance().getPlayer(playerName, playerUUID);
        for(Feature feature : features) {
            feature.onPlayerConnect(player);
        }
    }
    public void onPlayerDisconnect(String playerName, String playerUUID) {
        if(disabled) return;

        Player player = DataStorage.getInstance().getPlayer(playerName, playerUUID);
        for(Feature feature : features) {
            feature.onPlayerDisconnect(player);
        }
    }

    private void CreateDirectoryIfNotExists(Path folder) {
        try {
            if(!folder.toFile().exists()) {
                if(!folder.toFile().mkdir()) {
                    Logs.warn("Failed to create a directory");
                    disabled = true;
                    return;
                }
            }
        } catch(Exception exception) {
            Logs.warn("Failed to create a directory");
            disabled = true;
            return;
        }
    }
    private void CreateFileIfNotExists(File file, String useResource) {
        if(file.exists()) return;
    
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(useResource)) {
            if(is == null) {
                Logs.warn("Failed to find the resource to create file");
                disabled = true;
                return;
            }
            Files.copy(is, file.toPath());
        } catch (IOException e) {
            Logs.warn("Failed to create a file from a resource");
            disabled = true;
            return;
        }
    }
    void StoreToDisk(boolean backup) {
        Path dataDirectory = implementation.getDataDirectory();
        File dataFile = dataDirectory.resolve("data.yml").toFile();
        if(!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch(Exception ignored) {
                Logs.warn("Failed to create the data file");
            }
        }
        DataStorage.getInstance().isBackup = backup;
        DataStorage.getInstance().storedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString();
        DataStorage.getInstance().storeToDisk(dataFile);
    }

    boolean disabled = false;
    public Implementation implementation;
    public List<Feature> features = new ArrayList<>();
    // Scheduling:
    Timer timer = new Timer();
}