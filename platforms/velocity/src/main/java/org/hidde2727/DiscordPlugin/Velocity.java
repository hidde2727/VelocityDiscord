package org.hidde2727.DiscordPlugin;

import java.nio.file.Path;
import java.util.Optional;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
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

@Plugin(id = "discordio", name = "Discordio")// Not processed by velocity annotation processor, using the velocity-plugin.json resource instead
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
    public void onServerStart(ProxyInitializeEvent event) {
        plugin.onServerStart();
    }
    @Subscribe
    public void onServerStop(ProxyShutdownEvent event) {
        plugin.onServerStop();
    }
    @Subscribe
    public void onPlayerMessage(PlayerChatEvent event) {
        Player player = event.getPlayer();
        plugin.onPlayerMessage(
            player.getCurrentServer().get().getServerInfo().getName(), 
            player.getUsername(), 
            UuidUtils.toUndashed(player.getUniqueId()),
            event.getMessage()
        );
    }
    @Subscribe
    public void onPlayerPreLogin(PreLoginEvent event) {
        boolean letThrough = plugin.onPlayerPreLogin(
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
    public void onPlayerConnect(ServerConnectedEvent event) {
        if(event.getPreviousServer().isPresent()) return;// Player is changing internal server, do nothing

        Player player = event.getPlayer();
        plugin.onPlayerConnect(
            player.getUsername(), 
            UuidUtils.toUndashed(player.getUniqueId())
        );
    }
    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        plugin.onPlayerDisconnect(
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
    public Path getDataDirectory() {
        return dataDirectory;
    }
    public boolean isOnlineMode() {
        return server.getConfiguration().isOnlineMode();
    }
    public void sendMessage(String serverID, String message) {
        Optional<RegisteredServer> server = this.server.getServer(serverID);
        if(server.isEmpty()) {
            Logs.error("Cannot send a message to a server that is not register in the velocity.toml file");
            return;
        }
        server.get().sendMessage(Component.text(message));
    }
}
