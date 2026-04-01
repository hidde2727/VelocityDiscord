package org.hidde2727.DiscordPlugin.Features;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.hidde2727.DiscordPlugin.*;
import org.hidde2727.DiscordPlugin.Discord.Discord;
import org.hidde2727.DiscordPlugin.Storage.Config;
import org.hidde2727.DiscordPlugin.Storage.DataStorage;
import org.hidde2727.DiscordPlugin.Storage.Language;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class Maintenance extends ListenerAdapter {
    Language language;
    Discord discord;
    Config.Maintenance config;
    DataStorage.Maintenance permanentData;
    PlayerManager players;
    String commandName;
    String commandOptionName;

    public Maintenance(DiscordPlugin plugin) {
        this.language = plugin.language;
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
            Language.Command cmdLanguage = plugin.language.commands.get("maintenance");
            if(cmdLanguage == null) {
                Logs.error("Maintenance command missing from the language.yml file");
                return;
            }
            Language.Command.Option optLanguage = cmdLanguage.options.get("startStop");
            if(optLanguage == null) {
                Logs.error("Maintenance command options missing from the language.yml file");
                return;
            }
            if(optLanguage.options.size() < 2) {
                Logs.error("Maintenance command not enough options for the first options of the language.yml");
                return;
            }

            commandName = plugin.stringProcessor.GetString(cmdLanguage.name);
            commandOptionName = plugin.stringProcessor.GetString(optLanguage.name);

            Predicate<String> pattern = Pattern.compile("^[a-z-]+$").asMatchPredicate();
            if(!pattern.test(commandName) || !pattern.test(commandOptionName)) {
                Logs.warn("maintenanceCommand.name and maintenanceCommand.option.name in the messages.properties file may only contain lowercase letters and dashes (-)");
                config.command.enabled = false;
                return;
            }

            discord.AddCommand(Commands.slash(
                commandName,
                plugin.stringProcessor.GetString(cmdLanguage.description)
                )
                .addOptions(
                    (new OptionData(
                        OptionType.STRING,
                        commandOptionName,
                        plugin.stringProcessor.GetString(optLanguage.description),
                        true
                    )
                    .addChoice(
                        plugin.stringProcessor.GetString(optLanguage.options.get(0)),
                        "start"
                    )
                    .addChoice(
                        plugin.stringProcessor.GetString(optLanguage.options.get(1)),
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
                .SetLanguageNamespace("maintenance", "onStart")
                .SendInChannel(config.onStart.channel);
    }
    void OnDisable() {
        if(!config.enabled) return;
        if(!config.onStop.enabled) return;

        discord.CreateEmbed()
                .SetLanguageNamespace("maintenance", "onStop")
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
        if(event.getOption(commandOptionName) == null) {
            SendIncompleteMessage(event);
            return;
        }

        if(config.command.checkChannel) {
            if(!config.command.allowedChannels.contains(event.getChannelId())) {
                discord.CreateEmbed()
                    .SetLanguageNamespace("maintenance", "wrongChannel")
                    .SetVariables(GetVariables(event.getUser()))
                    .Send(event, true);
                return;
            }
        }
        if(config.command.checkRoles) {
            if(!discord.DoesUserHaveRole(event.getUser(), config.command.allowedRoles)) {
                discord.CreateEmbed()
                    .SetLanguageNamespace("maintenance", "notAllowed")
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
            .SetLanguageNamespace("maintenance", "command")
            .SetVariables(GetVariables(event.getUser()))
            .SetVariable("COMMAND_OPTION", event.getOption(commandOptionName).getAsString())
            .Send(event, true);
    }

    private void SendIncompleteMessage(SlashCommandInteractionEvent event) {
        discord.CreateEmbed()
                .SetLanguageNamespace("infoCommand", "incomplete")
                .SetVariables(GetVariables(event.getUser()))
                .Send(event, true);
    }
}
