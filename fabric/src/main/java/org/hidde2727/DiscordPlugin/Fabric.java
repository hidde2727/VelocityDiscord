package org.hidde2727.DiscordPlugin;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.hidde2727.DiscordPlugin.Implementation.Implementation;
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

        ServerLifecycleEvents.SERVER_STARTED.register(this::OnServerStart);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::OnServerStop);
        ServerPlayerEvents.JOIN.register(this::OnPlayerConnect);
        ServerPlayerEvents.LEAVE.register(this::OnPlayerDisconnect);
        ServerMessageEvents.CHAT_MESSAGE.register(this::OnPlayerMessage);
    }


    public void OnServerStart(MinecraftServer server) {
        this.server = server;
        plugin.OnServerStart();
    }
    public void OnServerStop(MinecraftServer server) {
        plugin.OnServerStop();
    }
    public void OnPlayerMessage(PlayerChatMessage message, ServerPlayer player, ChatType.Bound params) {
        plugin.OnPlayerMessage(
                "fabric",
                player.getPlainTextName(),
                player.getStringUUID(),
                message.signedContent()
        );
    }
    // OnPlayerPreLogin handled by FabricMixin
    public void OnPlayerConnect(ServerPlayer player) {
        plugin.OnPlayerConnect(
                player.getPlainTextName(),
                player.getStringUUID()
        );
    }
    public void OnPlayerDisconnect(ServerPlayer player) {
        plugin.OnPlayerDisconnect(
                player.getPlainTextName(),
                player.getStringUUID()
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
        return FabricLoader.getInstance().getConfigDir().resolve("discordio");
    }
    public boolean IsOnlineMode() {
        return server.usesAuthentication();
    }
    public void SendMessage(String serverID, String message) {
        if(!serverID.equals("fabric")) {
            Logs.error("Cannot send a message to a server other than the server with the id 'fabric' (check the onMessage event in your config.yml, it may only contain fabric)");
            return;
        }
        server.sendSystemMessage(Component.nullToEmpty(message));
    }
}
