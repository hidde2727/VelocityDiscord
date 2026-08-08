package org.hidde2727.DiscordPlugin.Discord;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;

import java.util.concurrent.CompletableFuture;

public class Embed {

    /**
     * Depending on the config either:
     * - Disables all the actions on the embed.
     * - Or deletes the message.
     */
    public void disable() {

    }
    /**
     * Depending on the config either:
     * - Enables all the actions on the embed.
     * - Or creates the message again.
     */
    public void enable() {

    }

    /** Modifies the content of the embed */
    public void modify(EmbedInfo info) {
        getSelf().whenCompleteAsync(((self, throwable) -> {
            MessageEditBuilder edit = MessageEditBuilder.fromMessage(self)
                    .setEmbeds(info.build())
                    .setReplace(true);
            info.addActionRows(edit);
            Discord.getInstance().queue(
                    self.editMessage(edit.build())
            );
        }));
    }

    CompletableFuture<Message> getSelf() {
        CompletableFuture<Message> future = new CompletableFuture<>();
        RestAction<Message> action = Discord.getInstance().jda
                .getTextChannelById(channelId)
                .retrieveMessageById(messageId);
        Discord.getInstance().queue(action, future::complete);
        return future;
    }

    EmbedInfo info;
    String channelId;
    String messageId;
}
