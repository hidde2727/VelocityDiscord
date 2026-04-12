package org.hidde2727.DiscordPlugin.Discord;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import org.hidde2727.DiscordPlugin.Logs;
import org.hidde2727.DiscordPlugin.StringProcessor;
import org.hidde2727.DiscordPlugin.Storage.Language;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.components.Component;
import net.dv8tion.jda.api.components.MessageTopLevelComponentUnion;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

public class Discord {
    JDA jda;
    final StringProcessor stringProcessor;
    final Language languageConfig;
    public static class MessageID {
        public MessageID() {}
        public MessageID(String channelId, long messageId, String deleteKey) {
            this.channelId = channelId;
            this.messageId = messageId;
            this.deleteKey = deleteKey;
        }
        public String channelId;
        public long messageId;
        public String deleteKey;
    }
    final Map<String, MessageID> toDeleteOrDisable;
    String guildId;

    public Discord(String botToken, String guildId, StringProcessor processor, Language languageConfig, Map<String, MessageID> toDeleteOrDisable) throws Exception {
        this.languageConfig = languageConfig;
        this.stringProcessor = processor;
        this.toDeleteOrDisable = toDeleteOrDisable;
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

    public void Shutdown(boolean toDisable) {
        // Disable all the messages marked to be disabled:
        AtomicInteger remainingMessages = new AtomicInteger(0);
        for(MessageID messageID : toDeleteOrDisable.values()) {
            if(toDisable) {
                Message message = GetMessage(messageID);
                if(message == null) {
                    // Message does not exist anymore
                    toDeleteOrDisable.remove(messageID.deleteKey);
                    continue;
                }
                remainingMessages.incrementAndGet();
                DisableMessage(message, (m) -> { remainingMessages.decrementAndGet(); });
            } else {
                try {
                    remainingMessages.incrementAndGet();
                    jda.getTextChannelById(messageID.channelId).deleteMessageById(messageID.messageId)
                            .onSuccess((m) -> {
                                remainingMessages.decrementAndGet();
                            })
                            .queue();
                } catch(Exception ignored) { }
                toDeleteOrDisable.remove(messageID.deleteKey);
            }
        }
        Logs.info("Awaiting (maximum 3s) deletion/disabling of messages");
        LocalTime endWait = LocalTime.now().plusSeconds(3);
        while(remainingMessages.get() > 0) {
            if(endWait.isBefore(LocalTime.now())) {
                Logs.warn("Failed to delete/disable all messages in 3 seconds, shutting down (remaining messages '" + remainingMessages.get() + "')");
                break;
            }
            Thread.yield();
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
        TextChannel channel = jda.getTextChannelById(channelID);
        if(channel == null) {
            Logs.warn("Failed to get text channel '" + channelID + "', maybe it was deleted?");
            return new ArrayList<>();
        }
        return channel.getMembers();
    }

    public void DeleteMessageOnShutdown(MessageID messageID) {
        toDeleteOrDisable.put(messageID.deleteKey, messageID);
    }
    public void KeepMessageOnShutdown(MessageID messageID) {
        toDeleteOrDisable.remove(messageID.deleteKey);
    }

    public Message GetMessage(MessageID messageID) {
        TextChannel channel = jda.getTextChannelById(messageID.channelId);
        if(channel == null) {
            Logs.warn("Failed to get text channel '" + messageID.channelId + "', maybe it was deleted?");
            return null;
        }
        try {
            return channel.retrieveMessageById(messageID.messageId).complete();
        } catch(Exception exc) {
            return null;// Message does not exist
        }
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

    public void EnableMessage(Message message) {
        EnableMessage(message, null);
    }
    private void EnableMessage(Message message, Consumer<? super Message> onSuccess) {
        List<MessageTopLevelComponentUnion> newComponents = new ArrayList<>();
        boolean changes = false;
        for(MessageTopLevelComponentUnion component : message.getComponents()) {
            if(component.getType() == Component.Type.ACTION_ROW) {
                if(component.asActionRow().isEnabled()) continue;
                changes = true;
                newComponents.add((MessageTopLevelComponentUnion) component.asActionRow().asEnabled());
            }
        }
        if(!changes) {
            onSuccess.accept(message);
            return;
        }
        MessageEditData edit = MessageEditBuilder.fromMessage(message)
                .setComponents(newComponents)
                .setReplace(true)
                .build();
        MessageEditAction action = message.editMessage(edit);
        if(onSuccess != null) action.onSuccess(onSuccess).queue();
        else action.queue();
    }
    public void DisableMessage(Message message) {
        DisableMessage(message, null);
    }
    private void DisableMessage(Message message, Consumer<? super Message> onSuccess) {
        List<MessageTopLevelComponentUnion> newComponents = new ArrayList<>();
        boolean changes = false;
        for(MessageTopLevelComponentUnion component : message.getComponents()) {
            if(component.getType() == Component.Type.ACTION_ROW) {
                if(component.asActionRow().isDisabled()) continue;
                changes = true;
                newComponents.add((MessageTopLevelComponentUnion) component.asActionRow().asDisabled());
            }
        }
        if(!changes) {
            onSuccess.accept(message);
            return;
        }
        MessageEditData edit = MessageEditBuilder.fromMessage(message)
                .setComponents(newComponents)
                .setReplace(true)
                .build();
        MessageEditAction action = message.editMessage(edit);
        if(onSuccess != null) action.onSuccess(onSuccess).queue();
        else action.queue();
    }
}
