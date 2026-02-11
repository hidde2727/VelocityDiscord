package org.hidde2727.DiscordPlugin;

import java.nio.file.Path;
import java.util.Optional;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import org.hidde2727.DiscordPlugin.Implementation.Implementation;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.util.UuidUtils;

@Plugin(id = "discordio", name = "Discordio", version = "3.5.0-SNAPSHOT",
        url = "", description = "Discord management of your velocity proxy", authors = {"hidde2727"})
public class Velocity implements Implementation {
    ProxyServer server;
    Logger logger;
    Path dataDirectory;
    DiscordPlugin plugin;

    @Inject
    public Velocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;

        plugin = new DiscordPlugin(this);
    }

    @Subscribe
    public void OnServerStart(ProxyInitializeEvent event) {
        plugin.OnServerStart();
    }
    @Subscribe
    public void OnServerStop(ProxyShutdownEvent event) {
        plugin.OnServerStop();
    }
    @Subscribe
    public void OnPlayerMessage(PlayerChatEvent event) {
        Player player = event.getPlayer();
        plugin.OnPlayerMessage(
            player.getCurrentServer().get().getServerInfo().getName(), 
            player.getUsername(), 
            UuidUtils.toUndashed(player.getUniqueId()),
            event.getMessage()
        );
    }
    @Subscribe
    public void OnPlayerPreLogin(PreLoginEvent event) {
        boolean letThrough = plugin.OnPlayerPreLogin(
                event.getUsername(),
                UuidUtils.toUndashed(event.getUniqueId())
        );
        if(!letThrough) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(
                Component.text("You are not whitelisted on this server")
            ));
        }
        // It is allowed, don't touch the event
    }
    @Subscribe
    public void OnPlayerConnect(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        plugin.OnPlayerConnect(
            player.getUsername(), 
            UuidUtils.toUndashed(player.getUniqueId())
        );
    }
    @Subscribe
    public void OnPlayerDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        plugin.OnPlayerDisconnect(
            player.getUsername(), 
            UuidUtils.toUndashed(player.getUniqueId())
        );
    }


    public void debug(String message) {
        logger.debug(message);
    }
    public void info(String message) {
        logger.info(message);
    }
    public void warn(String message) {
        logger.warn(message);
    }
    public void error(String message) {
        logger.error(message);
    }
    public Path GetDataDirectory() {
        return dataDirectory;
    }
    public boolean IsOnlineMode() {
        return server.getConfiguration().isOnlineMode();
    }
    public void SendMessage(String serverID, String message) {
        Optional<RegisteredServer> server = this.server.getServer(serverID);
        if(server.isEmpty()) {
            Logs.error("Cannot send a message to a server that is not register in the velocity.toml file");
            return;
        }
        server.get().sendMessage(Component.text(message));
    }
}
