package org.hidde2727.VelocityDiscordPlugin.Discord;

import java.awt.Color;
import java.time.LocalDateTime;
import java.util.function.Consumer;

import org.hidde2727.VelocityDiscordPlugin.StringProcessor;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class Embed {
    private Discord discord;
    private StringProcessor processor;
    private StringProcessor.VariableMap variables = new StringProcessor.VariableMap();
    private String namespace;
    private int maxNamespaceSearchDepth;
    private boolean setTimestamp = false;
    private boolean deleteOnShutdown = false;

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

    private MessageEmbed Build() {
        if(namespace == null) return null;

        this.processor = discord.GetStringProcessor().AddVariables(variables, 50);

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Placeholder title");
        embed.setTitle(ProcessString("title"));
        embed.setDescription(ProcessString("description"));
        embed.setFooter(ProcessString("footer.message"), ProcessString("footer.iconUrl"));
        if(setTimestamp) embed.setTimestamp(LocalDateTime.now());
        embed.setImage(ProcessString("imageUrl"));
        embed.setThumbnail(ProcessString("thumbnailUrl"));
        embed.setAuthor(ProcessString("author.name"), ProcessString("author.url"), ProcessString("author.iconUrl"));
        embed.setUrl(ProcessString("url"));
        embed.setColor(ProcessColor("color"));
        return embed.build();
    }
    public void SendInChannel(String channelId) {
        Consumer<Message> onSuccess = null;
        if(deleteOnShutdown) {
            onSuccess = (message) -> {
                toDelete.add(new MessageID(channelId, message.getIdLong()));
            };
        }
        jda.getTextChannelById(channelId).sendMessageEmbeds(this.Build()).queue(onSuccess);
    }
};