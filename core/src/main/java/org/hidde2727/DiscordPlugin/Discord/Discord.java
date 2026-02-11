package org.hidde2727.DiscordPlugin.Discord;

import java.util.ArrayList;
import java.util.List;

import org.hidde2727.DiscordPlugin.StringProcessor;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class Discord {
    JDA jda;
    final StringProcessor stringProcessor;
    public static class MessageID {
        public MessageID(String channelId, long messageId) {
            this.channelId = channelId;
            this.messageId = messageId;
        }
        String channelId;
        long messageId;
    };
    List<MessageID> toDelete = new ArrayList<>();

    public Discord(String botToken, StringProcessor processor) throws Exception {
        jda = JDABuilder.createDefault(botToken)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .build()
                .awaitReady();
        this.stringProcessor = processor;
    }

    public void Shutdown() {
        // Delete all the messages marked to be deleted:
        for(MessageID messageId: toDelete) {
            jda.getTextChannelById(messageId.channelId).deleteMessageById(messageId.messageId).complete();
        }
        // Stop JDA
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
        return jda.retrieveUserById(id).complete();
    }

    public boolean GiveUserRole(Guild guild, String userId, String role) {
        Role guildRole = guild.getRoleById(role);
        if(guildRole == null) return false;
        guild.addRoleToMember(guild.getMemberById(userId), guildRole).queue();
        return true;
    }

    public List<Member> GetUsersInChannel(String channelID) {
        return jda.getTextChannelById(channelID).getMembers();
    }

    public Member GetUserInChannel(String channelID, Long userID) {
        return jda.getTextChannelById(channelID).getMembers()
            .stream().filter((Member m) -> m.getIdLong() == userID)
            .toList().get(0);
    }

    public void KeepMessageOnShutdown(MessageID messageID) {
        toDelete.remove(messageID);
    }

    public Message GetMessage(String channelID, Long messageID) {
        return jda.getTextChannelById(channelID).retrieveMessageById(messageID).complete();
    }

    // Checks if the user has any of the roles
    public boolean DoesUserHaveRoleInChannel(String channelID, Long userID, List<String> roles) {
        for(Role role : GetUserInChannel(channelID, userID).getRoles()) {
            if(roles.contains(role.getName())) return true;
        }
        return false;
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

    public Modal CreateModal(String id) {
        return new Modal(this, id);
    }
}
