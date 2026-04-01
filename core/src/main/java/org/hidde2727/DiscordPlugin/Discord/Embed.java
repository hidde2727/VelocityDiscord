package org.hidde2727.DiscordPlugin.Discord;

import java.awt.Color;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.hidde2727.DiscordPlugin.Logs;
import org.hidde2727.DiscordPlugin.StringProcessor;
import org.hidde2727.DiscordPlugin.Discord.Discord.MessageID;

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
    private final Discord discord;
    private StringProcessor processor;
    private final StringProcessor.VariableMap variables = new StringProcessor.VariableMap();
    private Map<String, String> translations;
    private final List<ActionRow> actionRows = new ArrayList<>();
    private Consumer<MessageID> onSend = null;
    private boolean setTimestamp = false;
    private boolean deleteOnShutdown = false;

    Embed(Discord discord) {
        this.discord = discord;
    }

    private String ProcessString(String key) {
        return processor.ProcessVariables(translations.get(key));
    }
    private Color ProcessColor(String key) {
        return processor.GetColor(translations.get(key));
    }

    public Embed SetLanguageNamespace(String namespace, String key) {
        this.translations = discord.GetLanguage().GetEmbed(namespace, key);
        if(translations.isEmpty()) {
            Logs.warn("An embed with translation keys ['" + namespace + "', '" + key + "'] requested but no translations were not found");
        }
        return this;
    }
    public Embed SetVariable(String key, String value) {
        variables.Add(key, value);
        return this;
    }
    public Embed SetVariables(Map<String, String> variables) {
        for(Map.Entry<String, String> entry : variables.entrySet()) {
            this.variables.Add(entry.getKey(), entry.getValue());
        }
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
    public Embed OnSend(Consumer<MessageID> onSend) {
        this.onSend = onSend;
        return this;
    }
    public String GetTranslation(String key) {
        return translations.get(key);
    }
    public void AddToTranslation(String key, String value) {
        translations.put(key, translations.get(key) + value);
    }

    private MessageEmbed Build() {
        if(translations == null) return null;

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
                MessageID messageID = new MessageID(message.getChannelId(), message.getIdLong());
                discord.DeleteMessageOnShutdown(messageID);
                if(onSend != null) {
                    onSend.accept(messageID);
                }
            };
        }
        try {
            MessageCreateAction message = discord.jda.getTextChannelById(channelId).sendMessageEmbeds(this.Build());
            for(ActionRow row : actionRows) {
                row.AddTo(message, processor, translations);
            }
            message.queue(onSuccess);
        } catch(Exception exc) {
            Logs.warn("Failed to send an embed: " + exc.getMessage());
        }
    }
    public void Send(IReplyCallback callback, boolean ephermal) {
        Consumer<InteractionHook> onSuccess = null;
        if(deleteOnShutdown && !ephermal) {
            onSuccess = (message) -> {
                Interaction interaction = message.getInteraction();
                MessageID messageID = new MessageID(interaction.getChannelId(), message.getIdLong());
                discord.DeleteMessageOnShutdown(messageID);
                if(onSend != null) {
                    onSend.accept(messageID);
                }
            };
        }
        try {
            ReplyCallbackAction message = callback.replyEmbeds(this.Build());
            for(ActionRow row : actionRows) {
                row.AddTo(message, processor, translations);
            }
            message.setEphemeral(ephermal).queue(onSuccess);
        } catch(Exception exc) {
            Logs.warn("Failed to send an embed: " + exc.getMessage());
        }
    }
    public void ModifyHook(InteractionHook hook) {
        Consumer<Message> onSuccess = null;
        if(deleteOnShutdown) {
            onSuccess = (message) -> {
                MessageID messageID = new MessageID(message.getChannelId(), message.getIdLong());
                discord.DeleteMessageOnShutdown(messageID);
                if(onSend != null) {
                    onSend.accept(messageID);
                }
            };
        }
        try {
            MessageEditData edit = MessageEditBuilder.fromMessage(hook.getCallbackResponse().getMessage())
                    .setEmbeds(this.Build())
                    .setComponents(this.actionRows.stream().map((row)->row.Build(processor, translations)).toList())
                    .setReplace(true)
                    .build();
            hook.editOriginal(edit).queue(onSuccess);
        } catch(Exception exc) {
            Logs.warn("Failed to send an embed: " + exc.getMessage());
        }
    }
    public void Modify(Message message) {
        Consumer<Message> onSuccess = null;
        if(deleteOnShutdown) {
            onSuccess = (messageParam) -> {
                MessageID messageID = new MessageID(message.getChannelId(), message.getIdLong());
                discord.DeleteMessageOnShutdown(messageID);
                if(onSend != null) {
                    onSend.accept(messageID);
                }
            };
        }

        try {
            MessageEditData edit = MessageEditBuilder.fromMessage(message)
            .setEmbeds(this.Build())
            .setComponents(this.actionRows.stream().map((row)->row.Build(processor, translations)).toList())
            .setReplace(true)
            .build();
            message.editMessage(edit).queue(onSuccess);
        } catch(Exception exc) {
            Logs.warn("Failed to modify an embed: " + exc.getMessage());
        }
    }
}