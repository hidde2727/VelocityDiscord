package org.hidde2727.VelocityDiscordPlugin;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.hidde2727.VelocityDiscordPlugin.Discord.Discord;
import org.hidde2727.VelocityDiscordPlugin.Features.*;

@Plugin(id = "discord-velocity", name = "Velocity Discord", version = "3.5.0-SNAPSHOT",
        url = "", description = "Discord management of your velocity proxy", authors = {"hidde2727"})
public class VelocityDiscordPlugin {
    @Inject
    public VelocityDiscordPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        Logs.logger = logger;

        // Create the config directory
        try {
            dataDirectory.toFile().mkdir();
        } catch(Exception exception) {
            this.logger.warn("Failed to create the config directory");
            return;
        }
        // Create the config file if it does not exist
        File configFile = dataDirectory.resolve("config.yml").toFile();
        if(!configFile.exists()) {
            try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("config.yml")) {
                if(is == null) {
                    this.logger.warn("Failed to find the config file resource");
                    return;
                }
                Files.copy(is, configFile.toPath());
            } catch (IOException e) {
                this.logger.warn("Failed to create a config file");
                return;
            }
        }
        this.config = Config.Load(configFile);
        this.dataStorage = DataStorage.Load(dataDirectory.resolve("data.yml").toFile());
        discord = new Discord(config.botToken, ResourceBundle.getBundle("messages"));

        // Add all the features
        this.onStart = new OnStart(discord, config.events.onStart);
        this.onStop = new OnStop(discord, config.events.onStop);
        this.onJoin = new OnJoin(discord, config.events.onJoin);
        this.onLeave = new OnLeave(discord, config.events.onLeave);
        this.onMessage = new OnMessage(discord, config.events.onMessage);
        this.whitelist = new Whitelist(discord, config.whitelist, dataStorage.whitelist);
    }

    @Subscribe(priority = 0)
    public void onInitialize(ProxyInitializeEvent event) {
        this.onStart.onInitialize(event);
        server.getEventManager().register(this, this.onJoin);
        server.getEventManager().register(this, this.onLeave);
        server.getEventManager().register(this, this.onMessage);
        discord.AddEventListener(onMessage);
        this.whitelist.onInitialize(event);
        server.getEventManager().register(this, this.whitelist);
        discord.AddEventListener(whitelist);

    }
    @Subscribe
    public void onShutdown(ProxyShutdownEvent event) {
        onStop.OnShutdown(event);
        discord.Shutdown();

        File dataFile = dataDirectory.resolve("data.yml").toFile();
        if(!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch(Exception ignored) {
                this.logger.warn("Failed to create the data file");
            }
        }
        dataStorage.Unload(dataDirectory.resolve("data.yml").toFile());
    }

    private final ProxyServer server;
    private final Logger logger;
    private Config config = new Config();
    private DataStorage dataStorage = new DataStorage();
    private Discord discord;
    private Path dataDirectory;
    // Features:
    OnStart onStart;
    OnStop onStop;
    OnJoin onJoin;
    OnLeave onLeave;
    OnMessage onMessage;
    Whitelist whitelist;
}