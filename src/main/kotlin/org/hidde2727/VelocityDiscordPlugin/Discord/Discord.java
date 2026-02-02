package org.hidde2727.VelocityDiscordPlugin.Discord;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import org.hidde2727.VelocityDiscordPlugin.StringProcessor;
import org.hidde2727.VelocityDiscordPlugin.StringProcessor.VariableMap;

import java.awt.Color;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class Discord {
    private JDA jda;
    // String replacement variables:
    private StringProcessor.VariableMap globalVariables = new StringProcessor.VariableMap();
    private StringProcessor stringProcessor;
    class MessageID {
        MessageID(String channelId, long messageId) {
            this.channelId = channelId;
            this.messageId = messageId;
        }
        String channelId;
        long messageId;
    };
    private List<MessageID> toDelete = new ArrayList<>();

    public Discord(String botToken, ResourceBundle localization) {
        try {
            jda = JDABuilder.createDefault(botToken)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                    .build()
                    .awaitReady();
        } catch(Exception ignored) { }
        stringProcessor = new StringProcessor(globalVariables, localization);

        globalVariables.AddFunction("CURRENT_DATE", () -> { return LocalDate.now().toString(); });
        globalVariables.AddFunction("CURRENT_TIME", () -> { return LocalTime.now().truncatedTo(ChronoUnit.MINUTES).toString(); });
        globalVariables.AddFunction("CURRENT_DATE_TIME", () -> { return LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).toString(); });
        globalVariables.AddFunction("CURRENT_NANO_SECONDS", () -> { return String.valueOf(LocalDateTime.now().getNano()); });
        globalVariables.AddFunction("CURRENT_SECOND", () -> { return String.valueOf(LocalDateTime.now().getSecond()); });
        globalVariables.AddFunction("CURRENT_MINUTE", () -> { return String.valueOf(LocalDateTime.now().getMinute()); });
        globalVariables.AddFunction("CURRENT_HOUR", () -> { return String.valueOf(LocalDateTime.now().getHour()); });
        globalVariables.AddFunction("CURRENT_DAY", () -> { return String.valueOf(LocalDateTime.now().getDayOfMonth()); });
        globalVariables.AddFunction("CURRENT_MONTH", () -> { return String.valueOf(LocalDateTime.now().getMonthValue()); });
        globalVariables.AddFunction("CURRENT_YEAR", () -> { return String.valueOf(LocalDateTime.now().getYear()); });
    }
    public void Shutdown() {
        // Delete all the messages marked to be deleted:
        for(MessageID messageId: toDelete) {
            jda.getTextChannelById(messageId.channelId).deleteMessageById(messageId.messageId);
        }
        jda.shutdown();
    }

    public boolean DoesTextChannelExist(String id) {
        return jda.getTextChannelById(id) != null;
    }

    public void AddEventListener(Object... listeners) {
        jda.addEventListener(listeners);
    }

    StringProcessor GetStringProcessor() {
        return stringProcessor;
    }
    
    public Embed CreateEmbed() {
        return new Embed(this);
    }
}
