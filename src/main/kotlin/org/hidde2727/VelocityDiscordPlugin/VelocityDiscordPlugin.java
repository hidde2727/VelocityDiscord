package org.hidde2727.VelocityDiscordPlugin;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.hidde2727.VelocityDiscordPlugin.Discord.Discord;
import org.hidde2727.VelocityDiscordPlugin.Features.*;

@Plugin(id = "discord-velocity", name = "Velocity Discord", version = "3.5.0-SNAPSHOT",
        url = "", description = "Discord management of your velocity proxy", authors = {"hidde2727"})
public class VelocityDiscordPlugin {
    @Inject
    public VelocityDiscordPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;

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
        // Load the config
        Yaml yaml = new Yaml(new Constructor(Config.class, new LoaderOptions()));
        try {
            config = (Config) yaml.load(new FileInputStream(configFile));
        } catch(Exception exc) {
            this.logger.warn("Failed to parse the config file");
            this.logger.warn(exc.getMessage());
            return;
        }

        discord = new Discord(config.botToken, ResourceBundle.getBundle("messages"));

        // Add all the features
        this.onStart = new OnStart(discord, config.events.onStart, logger);
        this.onStop = new OnStop(discord, config.events.onStop, logger);
        this.onJoin = new OnJoin(discord, config.events.onJoin, logger);
        this.onLeave = new OnLeave(discord, config.events.onLeave, logger);
        this.onMessage = new OnMessage(discord, config.events.onMessage, logger);
    }

    @Subscribe
    public void onInitialize(ProxyInitializeEvent event) {
        this.onStart.onInitialize(event);
        server.getEventManager().register(this, this.onStop);
        server.getEventManager().register(this, this.onJoin);
        server.getEventManager().register(this, this.onLeave);
        server.getEventManager().register(this, this.onMessage);
    }
    @Subscribe
    public void onShutdown(ProxyShutdownEvent event) {
        discord.Shutdown();
    }

    private final ProxyServer server;
    private final Logger logger;
    private Config config = new Config();
    private Discord discord;
    // Features:
    OnStart onStart;
    OnStop onStop;
    OnJoin onJoin;
    OnLeave onLeave;
    OnMessage onMessage;
}