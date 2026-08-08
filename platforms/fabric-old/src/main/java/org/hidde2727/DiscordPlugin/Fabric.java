package org.hidde2727.DiscordPlugin;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class Fabric implements ModInitializer, Implementation {
    public final String MOD_ID = "discord-plugin";
    public final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static DiscordPlugin plugin;
    private MinecraftServer server;

    @Override
    public void onInitialize() {
        plugin = new DiscordPlugin(this);

        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStart);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStop);
        ServerPlayerEvents.JOIN.register(this::onPlayerConnect);
        ServerPlayerEvents.LEAVE.register(this::onPlayerDisconnect);
        ServerMessageEvents.CHAT_MESSAGE.register(this::onPlayerMessage);
    }


    public void onServerStart(MinecraftServer server) {
        this.server = server;
        plugin.onServerStart();
    }
    public void onServerStop(MinecraftServer server) {
        plugin.onServerStop();
    }
    public void onPlayerMessage(SignedMessage message, ServerPlayerEntity player, MessageType.Parameters params) {
        plugin.onPlayerMessage(
                "fabric",
                player.getStringifiedName(),
                player.getUuidAsString(),
                message.getSignedContent()
        );
    }
    // OnPlayerPreLogin handled by FabricMixin
    public void onPlayerConnect(ServerPlayerEntity player) {
        plugin.onPlayerConnect(
                player.getStringifiedName(),
                player.getUuidAsString()
        );
    }
    public void onPlayerDisconnect(ServerPlayerEntity player) {
        plugin.onPlayerDisconnect(
                player.getStringifiedName(),
                player.getUuidAsString()
        );
    }

    public void debug(String message) {
        LOGGER.debug(message);
    }
    public void info(String message) {
        LOGGER.info(message);
    }
    public void warn(String message) {
        LOGGER.warn(message);
    }
    public void error(String message) {
        LOGGER.error(message);
    }
    public Path getDataDirectory() {
        return FabricLoader.getInstance().getConfigDir().resolve("discordio");
    }
    public boolean isOnlineMode() {
        return server.isOnlineMode();
    }
    public void sendMessage(String serverID, String message) {
        if(!serverID.equals("fabric")) {
            Logs.error("Cannot send a message to a server other than the server with the id 'fabric' (check the onMessage event in your config.yml, it may only contain fabric)");
            return;
        }
        server.sendMessage(Text.of(message));
    }
}