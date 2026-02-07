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
import org.hidde2727.DiscordPlugin.Implementation.ActiveImplementation;
import org.hidde2727.DiscordPlugin.Implementation.Implementation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class Fabric implements ModInitializer, Implementation {
    public final String MOD_ID = "discord-plugin";
    public final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static DiscordPlugin plugin;

    @Override
    public void onInitialize() {
        ActiveImplementation.active = this;
        plugin = new DiscordPlugin();

        ServerLifecycleEvents.SERVER_STARTED.register(this::OnServerStart);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::OnServerStop);
        ServerPlayerEvents.JOIN.register(this::OnPlayerConnect);
        ServerPlayerEvents.LEAVE.register(this::OnPlayerDisconnect);
        ServerMessageEvents.CHAT_MESSAGE.register(this::OnPlayerMessage);
    }


    public void OnServerStart(MinecraftServer server) {
        plugin.OnServerStart();
    }
    public void OnServerStop(MinecraftServer server) {
        plugin.OnServerStop();
    }
    public void OnPlayerMessage(SignedMessage message, ServerPlayerEntity player, MessageType.Parameters params) {
        plugin.OnPlayerMessage(
                "Fabric-server",
                player.getStringifiedName(),
                player.getUuidAsString(),
                message.getSignedContent()
        );
    }
    // OnPlayerPreLogin handled by FabricMixin
    public void OnPlayerConnect(ServerPlayerEntity player) {
        plugin.OnPlayerConnect(
                player.getStringifiedName(),
                player.getUuidAsString()
        );
    }
    public void OnPlayerDisconnect(ServerPlayerEntity player) {
        plugin.OnPlayerDisconnect(
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
    public Path GetDataDirectory() {
        return FabricLoader.getInstance().getConfigDir().resolve("fabric-discord");
    }
    public boolean IsOnlineMode() {
        return true;//TODO
    }
}
