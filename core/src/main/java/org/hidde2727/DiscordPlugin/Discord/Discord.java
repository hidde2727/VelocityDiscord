package org.hidde2727.DiscordPlugin.Discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.RestAction;
import org.hidde2727.DiscordPlugin.Logs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class Discord {
    private Discord(String botToken, String guildId) throws Exception {
        jda = JDABuilder.createDefault(botToken)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .setEnableShutdownHook(false)
                .build()
                .awaitReady();
        this.guildId = guildId;
        if(jda.getGuildById(guildId) == null) {
            throw new Exception("Received a guild that does not exist");
        }
        Logs.info("Running in the '" + jda.getGuildById(guildId).getName() + "' guild");
    }

    private static Discord instance;
    public static Discord getInstance() {
        return instance;
    }
    public static void init(String botToken, String guildId) throws Exception {
        instance = new Discord(botToken, guildId);
    }
    public static void shutdown() {
        Logs.info("Shutting down JDA");
        // Make sure all the requests are completed:
        for(RestAction action : instance.activeRequests) {
            action.complete();
        }
        instance.jda.shutdown();
    }

    /// Returns a list of all the users in a text channel.
    public List<Member> getUsersInChannel(String channelID) {
        TextChannel channel = jda.getTextChannelById(channelID);
        if(channel == null) {
            Logs.warn("Failed to get text channel '" + channelID + "', maybe it was deleted?");
            return new ArrayList<>();
        }
        return channel.getMembers();
    }

    /// Checks if the user has any of the roles in the guild from the config
    public boolean doesUserHaveRole(User user, List<String> roles) {
        if(user == null) {
            Logs.warn("DoesUserHaveRole called with user==null");
            return false;
        }
        Guild guild = jda.getGuildById(guildId);
        if(guild == null) {
            Logs.warn("Could not get the guild '" + guildId + "'");
            return false;
        }
        Member member = guild.retrieveMember(user).complete();
        if(member == null) {
            Logs.warn("DoesUserHaveRole could not find member");
            return false;
        }
        for(Role role : member.getRoles()) {
            if(roles.contains(role.getName())) return true;
        }
        return false;
    }

    /// Returns if the channel exists
    public boolean doesTextChannelExist(String id) {
        try {
            return jda.getTextChannelById(id) != null;
        } catch(Exception ignored) {
            return false;
        }
    }
    /// Returns if the bot can access the channel
    public boolean canBotAccessTextChannel(String id) {
        if(!doesTextChannelExist(id)) return false;
        return jda.getTextChannelById(id).canTalk();
    }

    /// Checks if the text channel exists and if the bot has access to it:
    /// The channelName is used just for the error messages
    public boolean checkChannel(String channelId, String channelName) {
        if(!this.doesTextChannelExist(channelId)) {
            Logs.error(channelName + " channel does not exist");
            return false;
        } else if(!this.canBotAccessTextChannel(channelId)) {
            Logs.error("The bot cannot access the " + channelName + " channel");
            return false;
        }
        return true;
    }

    /// Returns the user id of the bot
    public String getSelfId() {
        return jda.getSelfUser().getId();
    }

    /// This method will make sure the rest action finishes before the bot shutdowns.
    public <T> void queue(RestAction<T> action) {
        queue(action, (message)->{});
    }
    /// This method will make sure the rest action finishes before the bot shutdowns.
    public <T> void queue(RestAction<T> action, Consumer<? super T> onSuccess) {
        activeRequests.add(action);
        action.queue((completed) -> {
            activeRequests.remove(action);
            onSuccess.accept(completed);
        });
    }

    JDA jda;
    String guildId;
    Set<RestAction> activeRequests = new HashSet<>();
}