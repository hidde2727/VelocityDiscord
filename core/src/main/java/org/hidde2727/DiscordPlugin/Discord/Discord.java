package org.hidde2727.DiscordPlugin.Discord;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hidde2727.DiscordPlugin.Logs;
import org.hidde2727.DiscordPlugin.StringProcessor;
import org.hidde2727.DiscordPlugin.Storage.Language;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class Discord {
    JDA jda;
    final StringProcessor stringProcessor;
    final Language languageConfig;
    public static class MessageID {
        public MessageID(String channelId, long messageId) {
            this.channelId = channelId;
            this.messageId = messageId;
        }
        public String channelId;
        public long messageId;
    }
    Map<Long, MessageID> toDelete = new HashMap<>();
    String guildId;

    public Discord(String botToken, String guildId, StringProcessor processor, Language languageConfig) throws Exception {
        this.languageConfig = languageConfig;
        this.stringProcessor = processor;
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

    public void Shutdown() {
        // Delete all the messages marked to be deleted:
        for(MessageID messageId: toDelete.values()) {
            try {
                jda.getTextChannelById(messageId.channelId).deleteMessageById(messageId.messageId).complete();
            } catch(Exception ignored) { }
        }
        // Stop JDA
        Logs.info("Shutting down JDA");
        jda.shutdown();
    }

    public boolean DoesTextChannelExist(String id) {
        try {
            return jda.getTextChannelById(id) != null;
        } catch(Exception ignored) {
            return false;
        }
    }
    public boolean CanBotAccesTextChannel(String id) {
        if(!DoesTextChannelExist(id)) return false;
        return jda.getTextChannelById(id).canTalk();
    }

    public String GetSelfId() {
        return jda.getSelfUser().getId();
    }

    public User GetUserByID(String id) {
        if(id == null) return null;
        return jda.retrieveUserById(id).complete();
    }

    public boolean GiveUserRole(String userId, String role) {
        Guild guild = jda.getGuildById(guildId);
        if(guild == null) {
            Logs.warn("Failed to find guild '" + guildId + "'");
            return false;
        }
        Role guildRole = guild.getRoleById(role);
        if(guildRole == null) {
            Logs.warn("Failed to find role in guild '" + role + "'");
            return false;
        }
        guild.retrieveMemberById(userId).onSuccess((member) -> {
            guild.addRoleToMember(member, guildRole).queue();
        }).queue();
        return true;
    }
    public boolean RemoveUserRole(String userId, String role) {
        Guild guild = jda.getGuildById(guildId);
        if(guild == null) {
            Logs.warn("Failed to find guild '" + guildId + "'");
            return false;
        }
        Role guildRole = guild.getRoleById(role);
        if(guildRole == null) {
            Logs.warn("Failed to find role in guild '" + role + "'");
            return false;
        }
        guild.retrieveMemberById(userId).onSuccess((member) -> {
            guild.removeRoleFromMember(member, guildRole).queue();
        }).queue();
        return true;
    }

    public List<Member> GetUsersInChannel(String channelID) {
        return jda.getTextChannelById(channelID).getMembers();
    }

    public void DeleteMessageOnShutdown(MessageID messageID) {
        toDelete.put(messageID.messageId, messageID);
    }
    public void KeepMessageOnShutdown(MessageID messageID) {
        toDelete.remove(messageID.messageId);
    }

    public Message GetMessage(MessageID messageID) {
        return jda.getTextChannelById(messageID.channelId).retrieveMessageById(messageID.messageId).complete();
    }
    public Message GetMessage(String channelID, Long messageID) {
        return jda.getTextChannelById(channelID).retrieveMessageById(messageID).complete();
    }

    // Checks if the user has any of the roles in the guild from the config
    public boolean DoesUserHaveRole(User user, List<String> roles) {
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

    public void AddCommand(CommandData command) {
        jda.getGuildById(guildId).upsertCommand(command).queue();
    }

    public void AddEventListener(Object... listeners) {
        jda.addEventListener(listeners);
    }

    StringProcessor GetStringProcessor() {
        return stringProcessor;
    }
    Language GetLanguage() { return languageConfig; }
    
    public Embed CreateEmbed() {
        return new Embed(this);
    }

    public Modal CreateModal(String id) {
        return new Modal(this, id);
    }
}
