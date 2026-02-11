package org.hidde2727.DiscordPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import org.hidde2727.DiscordPlugin.Discord.Discord;
import org.hidde2727.DiscordPlugin.Features.*;
import org.hidde2727.DiscordPlugin.Implementation.Implementation;

public class DiscordPlugin {
    public DiscordPlugin(Implementation implementation) {
        this.implementation = implementation;
        Logs.useForLogging = this;

        Path dataDirectory = implementation.GetDataDirectory();

        CreateDirectoryIfNotExists(dataDirectory);
        File configFile = dataDirectory.resolve("config.yml").toFile();
        CreateFileIfNotExists(configFile, "config.yml");
        File messageFile = dataDirectory.resolve("messages.properties").toFile();
        CreateFileIfNotExists(messageFile, "messages.properties");

        config = Config.Load(configFile);
        dataStorage = DataStorage.Load(dataDirectory.resolve("data.yml").toFile());

        SetupVariableMap();
        stringProcessor = StringProcessor.FromFile(globalVariables, dataDirectory.toFile(), "messages");

        try {
            discord = new Discord(config.botToken, stringProcessor);
        } catch(Exception exc) {
            Logs.warn(exc.getMessage());
            disabled = true;
            return;
        }
        players = new PlayerManager(config, dataStorage, implementation.IsOnlineMode());

        // Add all the features
        this.onStart = new OnStart(this);
        this.onStop = new OnStop(this);
        this.onJoin = new OnJoin(this);
        this.onLeave = new OnLeave(this);
        this.onMessage = new OnMessage(this);
        this.whitelist = new Whitelist(this);
    }

    public void OnServerStart() {
        if(disabled) return;

        this.onStart.OnServerStart();
        discord.AddEventListener(onMessage);
        this.whitelist.OnServerStart();
        discord.AddEventListener(whitelist);

    }
    public void OnServerStop() {
        discord.Shutdown();
        if(disabled) return;

        onStop.OnServerStop();

        Path dataDirectory = implementation.GetDataDirectory();
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
    private void SetupVariableMap() {
        globalVariables.AddFunction("CURRENT_DATE", () -> { return LocalDate.now().toString(); });
        globalVariables.AddFunction("CURRENT_TIME", () -> { return LocalTime.now().truncatedTo(ChronoUnit.MINUTES).toString(); });
        globalVariables.AddFunction("CURRENT_DATE_TIME", () -> { return LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).toString(); });
        globalVariables.AddFunction("CURRENT_NANO_SECONDS", () -> { return String.valueOf(LocalDateTime.now().getNano()); });
        globalVariables.AddFunction("CURRENT_SECOND", () -> { return String.valueOf(LocalDateTime.now().getSecond()); });
        globalVariables.AddFunction("CURRENT_MINUTE", () -> { return String.valueOf(LocalDateTime.now().getMinute()); });
        globalVariables.AddFunction("CURRENT_HOUR", () -> { return String.valueOf(LocalDateTime.now().getHour()); });
        globalVariables.AddFunction("CURRENT_DAY", () -> { return String.valueOf(LocalDateTime.now().getDayOfMonth()); });
        globalVariables.AddFunction("CURRENT_MONTH", () -> { return String.valueOf(LocalDateTime.now().getMonthValue()); });
        globalVariables.AddFunction("CURRENT_YEAR", () -> { return String.valueOf(LocalDateTime.now().getYear()); });
    }

    boolean disabled = false;
    public Config config = new Config();
    public DataStorage dataStorage = new DataStorage();
    public PlayerManager players;
    public Discord discord;
    public StringProcessor stringProcessor;
    public StringProcessor.VariableMap globalVariables = new StringProcessor.VariableMap();
    public Implementation implementation;
    // Features:
    OnStart onStart;
    OnStop onStop;
    OnJoin onJoin;
    OnLeave onLeave;
    OnMessage onMessage;
    Whitelist whitelist;
}