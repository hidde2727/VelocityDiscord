package org.hidde2727.VelocityDiscordPlugin.Discord;

import java.awt.Color;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.hidde2727.VelocityDiscordPlugin.StringProcessor;
import org.hidde2727.VelocityDiscordPlugin.Discord.Discord.MessageID;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

public class Embed {
    private Discord discord;
    private StringProcessor processor;
    private StringProcessor.VariableMap variables = new StringProcessor.VariableMap();
    private String namespace;
    private int maxNamespaceSearchDepth;
    private boolean setTimestamp = false;
    private boolean deleteOnShutdown = false;
    private List<ActionRow> actionRows = new ArrayList<>();
    private BiConsumer<String, Long> onSend = null;

    Embed(Discord discord) {
        this.discord = discord;
    }

    private String ProcessString(String key) {
        return processor.GetString(key, namespace, maxNamespaceSearchDepth);
    }
    private Color ProcessColor(String key) {
        return processor.GetColor(key, namespace, maxNamespaceSearchDepth);
    }

    public Embed SetLocalizationNamespace(String namespace, int maxSearchDepth) {
        this.namespace = namespace;
        this.maxNamespaceSearchDepth = maxSearchDepth;
        return this;
    }
    public Embed SetVariable(String key, String value) {
        variables.Add(key, value);
        return this;
    }
    public Embed SetTimestamp() {
        setTimestamp = true;
        return this;
    }
    public Embed DeleteOnShutdown() {
        deleteOnShutdown = true;
        return this;
    }
    public Embed AddActionRow(ActionRow row) {
        actionRows.add(row);
        return this;
    }
    public Embed OnSend(BiConsumer<String, Long> onSend) {
        this.onSend = onSend;
        return this;
    }

    private MessageEmbed Build() {
        if(namespace == null) return null;

        this.processor = discord.GetStringProcessor().AddVariables(variables, 50);

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(ProcessString("title"));
        embed.setDescription(ProcessString("description"));
        embed.setFooter(ProcessString("footer.message"), ProcessString("footer.iconUrl"));
        if(setTimestamp) embed.setTimestamp(LocalDateTime.now());
        embed.setImage(ProcessString("imageUrl"));
        embed.setThumbnail(ProcessString("thumbnailUrl"));
        embed.setAuthor(ProcessString("author.name"), ProcessString("author.url"), ProcessString("author.iconUrl"));
        embed.setUrl(ProcessString("url"));
        embed.setColor(ProcessColor("color"));
        if(embed.isEmpty()) embed.setTitle("Empty embed, no data was provided");
        return embed.build();
    }
    public void SendInChannel(String channelId) {
        Consumer<Message> onSuccess = null;
        if(deleteOnShutdown) {
            onSuccess = (message) -> {
                discord.toDelete.add(new MessageID(message.getChannelId(), message.getIdLong()));
                if(onSend != null) {
                    onSend.accept(message.getChannelId(), message.getIdLong());
                }
            };
        }
        MessageCreateAction message = discord.jda.getTextChannelById(channelId).sendMessageEmbeds(this.Build());
        for(ActionRow row : actionRows) {
            row.AddTo(message, processor, namespace, maxNamespaceSearchDepth);
        }
        message.queue(onSuccess);
    }
    public void Send(IReplyCallback callback, boolean ephermal) {
        Consumer<InteractionHook> onSuccess = null;
        if(deleteOnShutdown && !ephermal) {
            onSuccess = (message) -> {
                Interaction interaction = message.getInteraction();
                discord.toDelete.add(new MessageID(interaction.getChannelId(), interaction.getIdLong()));
                if(onSend != null) {
                    onSend.accept(interaction.getChannelId(), interaction.getIdLong());
                }
            };
        }
        ReplyCallbackAction message = callback.replyEmbeds(this.Build());
        for(ActionRow row : actionRows) {
            row.AddTo(message, processor, namespace, maxNamespaceSearchDepth);
        }
        message.setEphemeral(ephermal).queue(onSuccess);
    }
    public void Modify(Message message) {
        Consumer<Message> onSuccess = null;
        if(deleteOnShutdown) {
            onSuccess = (messageParam) -> {
                discord.toDelete.add(new MessageID(messageParam.getChannelId(), messageParam.getIdLong()));
                if(onSend != null) {
                    onSend.accept(messageParam.getChannelId(), messageParam.getIdLong());
                }
            };
        }

        MessageEditData edit = MessageEditBuilder.fromMessage(message)
        .setEmbeds(this.Build())
        .setComponents(this.actionRows.stream().map((row)->row.Build(processor, namespace, maxNamespaceSearchDepth)).toList())
        .setReplace(true)
        .build();
        message.editMessage(edit).queue(onSuccess);
    }
};