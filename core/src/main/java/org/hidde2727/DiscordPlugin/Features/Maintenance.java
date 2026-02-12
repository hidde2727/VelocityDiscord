package org.hidde2727.DiscordPlugin.Features;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.hidde2727.DiscordPlugin.*;
import org.hidde2727.DiscordPlugin.Discord.Discord;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class Maintenance extends ListenerAdapter {
    Discord discord;
    Config.Maintenance config;
    DataStorage.Maintenance permanentData;
    PlayerManager players;
    String commandName;
    String commandOptionName;

    public Maintenance(DiscordPlugin plugin) {
        this.discord = plugin.discord;
        this.config = plugin.config.maintenance;
        this.permanentData = plugin.dataStorage.maintenance;
        this.players = plugin.players;

        if(!config.enabled) return;

        if(config.inMaintenance) {
            if(!permanentData.configMaintenance) OnEnable();
            permanentData.configMaintenance = true;
        } else {
            if(permanentData.configMaintenance) OnDisable();
            permanentData.configMaintenance = false;
        }

        if(config.command.enabled) {
            Predicate<String> pattern = Pattern.compile("^[\\w-]+$").asMatchPredicate();
            commandName = plugin.stringProcessor.GetString("name", "maintenanceCommand", 1);
            commandOptionName = plugin.stringProcessor.GetString("name", "maintenanceCommand.option", 1);
            if(!pattern.test(commandName) || !pattern.test(commandOptionName)) {
                Logs.warn("maintenanceCommand.name and maintenanceCommand.option.name in the messages.properties file may only contain letters and dashes (-)");
                config.command.enabled = false;
                return;
            }
            discord.AddCommand(Commands.slash(
                commandName,
                plugin.stringProcessor.GetString("description", "maintenanceCommand", 1)
                )
                .addOptions(
                    (new OptionData(
                        OptionType.STRING,
                        commandOptionName,
                        plugin.stringProcessor.GetString("description", "maintenanceCommand.option", 1),
                        true
                    )
                    .addChoice(
                        plugin.stringProcessor.GetString("start", "maintenanceCommand", 1),
                        "start"
                    )
                    .addChoice(
                        plugin.stringProcessor.GetString("stop", "maintenanceCommand", 1),
                        "stop"
                    )
                )
            ));
        }
    }

    Map<String, String> GetVariables(User user) {
        Map<String, String> variables = new HashMap<>();
        variables.put("DISCORD_NAME", user.getName());
        variables.put("DISCORD_GLOBAL_NAME", user.getGlobalName());
        variables.put("DISCORD_EFFECTIVE_NAME", user.getEffectiveName());
        variables.put("DISCORD_UUID", user.getId());
        return variables;
    }

    void OnEnable() {
        if(!config.enabled) return;
        if(!config.onStart.enabled) return;
        
        discord.CreateEmbed()
                .SetLocalizationNamespace("embeds.onMaintenanceStart", 2)
                .SendInChannel(config.onStart.channel);
    }
    void OnDisable() {
        if(!config.enabled) return;
        if(!config.onStop.enabled) return;

        discord.CreateEmbed()
                .SetLocalizationNamespace("embeds.onMaintenanceStop", 2)
                .SendInChannel(config.onStop.channel);
    }

    public boolean OnPlayerPreLogin(String playerName, String playerUUID) {
        if(!config.enabled) return true;
        if(config.inMaintenance || permanentData.InMaintenance()) {
            // Check the players key
            String minecraftKey = players.GetMinecraftKey(playerName, playerUUID);
            return config.crew.contains(minecraftKey);
        }
        return true;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if(!config.enabled) return;
        if(!config.command.enabled) return;
        if (!event.getName().equals(commandName)) return;
        if(event.getOption(commandOptionName) == null) return;

        if(config.command.checkChannel) {
            if(!config.command.allowedChannels.contains(event.getChannelId())) {
                discord.CreateEmbed()
                    .SetLocalizationNamespace("embeds.maintenanceCommandWrongChannel", 2)
                    .SetVariables(GetVariables(event.getUser()))
                    .Send(event, true);
                return;
            }
        }
        if(config.command.checkRoles) {
            if(!discord.DoesUserHaveRoleInChannel(event.getChannelId(), event.getUser().getIdLong(), config.command.allowedRoles)) {
                discord.CreateEmbed()
                    .SetLocalizationNamespace("embeds.maintenanceCommandNotAllowed", 2)
                    .SetVariables(GetVariables(event.getUser()))
                    .Send(event, true);
                return;
            }
        }

        if(event.getOption(commandOptionName).getAsString().equals("start")) {
            if(!permanentData.discordCommandMaintenance) OnEnable();
            permanentData.discordCommandMaintenance = true;
        } else if(event.getOption(commandOptionName).getAsString().equals("stop")) {
            if(permanentData.discordCommandMaintenance) OnDisable();
            permanentData.discordCommandMaintenance = false;
        } else {
            Logs.warn("Received an unknown option to the maintenance command '" + event.getOption(commandOptionName).getAsString() + "'");
            return;
        }
        discord.CreateEmbed()
            .SetLocalizationNamespace("embeds.maintenanceCommand", 2)
            .SetVariables(GetVariables(event.getUser()))
            .SetVariable("COMMAND_OPTION", event.getOption(commandOptionName).getAsString())
            .Send(event, true);
    }
}
