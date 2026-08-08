package org.hidde2727.DiscordPlugin.Discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateRequest;
import net.dv8tion.jda.api.utils.messages.MessageData;
import net.dv8tion.jda.api.utils.messages.MessageRequest;
import org.hidde2727.DiscordPlugin.Logs;
import org.hidde2727.DiscordPlugin.Storage.Language;
import org.hidde2727.DiscordPlugin.StringProcessor;
import org.hidde2727.DiscordPlugin.VariableProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * All the info necessary to build an Embed.
 */
public class EmbedInfo {
    Language.Embed language;
    VariableProvider variables;
    List<ActionRow> actionRows;
    private transient StringProcessor stringProcessor;

    public EmbedInfo setLanguage(String namespace, String key) {
        language = Language.getInstance().getEmbed(namespace, key);
        if(language == null) {
            language = new Language.Embed();
            language.title = "Could not find the language for this embed";
            Logs.error("Could not find the language for the embed '" + namespace + "." + key + "'");
        }
        return this;
    }
    public EmbedInfo setVariables(VariableProvider variables) {
        this.variables = variables;
        return this;
    }
    public EmbedInfo addActionRow(ActionRow actionRow) {
        this.actionRows.add(actionRow);
        return this;
    }

    public CompletableFuture<Embed> sendInChannel(String channelId) {
        CompletableFuture<Embed> future = new CompletableFuture<>();
        MessageCreateAction action = Discord.getInstance().jda
                .getTextChannelById(channelId)
                .sendMessageEmbeds(build());
        addActionRows(action);
        Discord.getInstance().queue(action, (messageEvent) -> {
            Embed embed = new Embed();
            embed.info = this;
            embed.channelId = channelId;
            embed.messageId = messageEvent.getId();
            future.complete(embed);
        });
        return future;
    }
    public CompletableFuture<Embed> sendAsReply(IReplyCallback callback, boolean ephemeral) {
        CompletableFuture<Embed> future = new CompletableFuture<>();
        ReplyCallbackAction action = callback
                .replyEmbeds(build())
                .setEphemeral(ephemeral);
        addActionRows(action);
        Discord.getInstance().queue(action, (messageEvent) -> {
            Embed embed = new Embed();
            embed.info = this;
            embed.channelId = messageEvent.getInteraction().getChannelId();
            embed.messageId = messageEvent.getId();
            future.complete(embed);
        });
        return future;
    }





    MessageEmbed build() {
        if(language == null) {
            Logs.warn("!Embed without any language set!");
            return null;
        }

        stringProcessor = StringProcessor.getDefault();
        if(variables != null) {
            stringProcessor = stringProcessor.addVariables(variables.getVariables(), 0);
        }

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(
                stringProcessor.processVariables(language.title)
        );
        embed.setDescription(
                stringProcessor.processVariables(language.description)
        );
        embed.setFooter(
                stringProcessor.processVariables(language.footer.message),
                stringProcessor.processVariables(language.footer.iconUrl)
        );
        embed.setImage(
                stringProcessor.processVariables(language.imageUrl)
        );
        embed.setThumbnail(
                stringProcessor.processVariables(language.thumbnailUrl)
        );
        embed.setAuthor(
                stringProcessor.processVariables(language.author.name),
                stringProcessor.processVariables(language.author.url),
                stringProcessor.processVariables(language.author.iconUrl)
        );
        embed.setUrl(
                stringProcessor.processVariables(language.url)
        );
        embed.setColor(
                stringProcessor.getColor(language.color)
        );
        if(embed.isEmpty()) embed.setTitle("Empty embed (translation key: '" + language.translationKey + "'), no data was provided");
        return embed.build();
    }
    void addActionRows(MessageRequest messageRequest) {
        if(actionRows == null) return;

        List<net.dv8tion.jda.api.components.actionrow.ActionRow> rows = new ArrayList<>();
        for(ActionRow row : actionRows) {
            rows.add(row.build(stringProcessor, language.actions));
        }
        messageRequest.setComponents(rows);
    }
}
