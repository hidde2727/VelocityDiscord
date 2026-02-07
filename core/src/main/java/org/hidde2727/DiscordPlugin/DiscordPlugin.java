package org.hidde2727.DiscordPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;

import org.hidde2727.DiscordPlugin.Discord.Discord;
import org.hidde2727.DiscordPlugin.Features.*;
import org.hidde2727.DiscordPlugin.Implementation.ActiveImplementation;

public class DiscordPlugin {
    public DiscordPlugin() {
        Path dataDirectory = ActiveImplementation.active.GetDataDirectory();
        // Create the config directory
        try {
            if(!dataDirectory.toFile().exists()) {
                if(!dataDirectory.toFile().mkdir()) {
                    Logs.warn("Failed to create the config directory");
                    disabled = true;
                    return;
                }
            }
        } catch(Exception exception) {
            Logs.warn("Failed to create the config directory");
            disabled = true;
            return;
        }
        // Create the config file if it does not exist
        File configFile = dataDirectory.resolve("config.yml").toFile();
        if(!configFile.exists()) {

            Logs.info("No config file found, creating a new config file");
            try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("config.yml")) {
                if(is == null) {
                    Logs.warn("Failed to find the config file resource");
                    disabled = true;
                    return;
                }
                Files.copy(is, configFile.toPath());
            } catch (IOException e) {
                Logs.warn("Failed to create a config file");
                disabled = true;
                return;
            }
        }
        this.config = Config.Load(configFile);
        this.dataStorage = DataStorage.Load(dataDirectory.resolve("data.yml").toFile());
        try {
            discord = new Discord(config.botToken, ResourceBundle.getBundle("messages"));
        } catch(Exception exc) {
            Logs.warn(exc.getMessage());
            disabled = true;
            return;
        }
        this.players = new PlayerManager(config, dataStorage, ActiveImplementation.active.IsOnlineMode());

        // Add all the features
        this.onStart = new OnStart(discord, config.events.onStart);
        this.onStop = new OnStop(discord, config.events.onStop);
        this.onJoin = new OnJoin(discord, config.events.onJoin);
        this.onLeave = new OnLeave(discord, config.events.onLeave);
        this.onMessage = new OnMessage(discord, config.events.onMessage);
        this.whitelist = new Whitelist(discord, config.whitelist, dataStorage, players);
    }

    public void OnServerStart() {
        if(disabled) return;

        this.onStart.OnServerStart();
        discord.AddEventListener(onMessage);
        this.whitelist.OnServerStart();
        discord.AddEventListener(whitelist);

    }
    public void OnServerStop() {
        if(disabled) return;

        Path dataDirectory = ActiveImplementation.active.GetDataDirectory();
        onStop.OnServerStop();
        discord.Shutdown();

        File dataFile = dataDirectory.resolve("data.yml").toFile();
        if(!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch(Exception ignored) {
                Logs.warn("Failed to create the data file");
            }
        }
        dataStorage.Unload(dataDirectory.resolve("data.yml").toFile());
    }
    public void OnPlayerMessage(String onServer, String playerName, String playerUUID, String message) {
        if(disabled) return;

        onMessage.OnPlayerMessage(onServer, playerName, playerUUID, message);
    }
    public boolean OnPlayerPreLogin(String playerName, String playerUUID) {
        if(disabled) return false;
        Logs.info("Player with UUID: " + playerUUID);

        if(!whitelist.OnPlayerPreLogin(playerName, playerUUID)) return false;
        // TODO: Check bans
        return true;
    }
    public void OnPlayerConnect(String playerName, String playerUUID) {
        if(disabled) return;

        onJoin.OnPlayerConnect(playerName, playerUUID);
    }
    public void OnPlayerDisconnect(String playerName, String playerUUID) {
        if(disabled) return;

        onLeave.OnPlayerDisconnect(playerName, playerUUID);
    }

    boolean disabled = false;
    private Config config = new Config();
    private DataStorage dataStorage = new DataStorage();
    private PlayerManager players;
    private Discord discord;
    // Features:
    OnStart onStart;
    OnStop onStop;
    OnJoin onJoin;
    OnLeave onLeave;
    OnMessage onMessage;
    Whitelist whitelist;
}