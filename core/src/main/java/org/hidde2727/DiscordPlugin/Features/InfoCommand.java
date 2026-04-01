package org.hidde2727.DiscordPlugin.Features;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.*;
import org.hidde2727.DiscordPlugin.*;
import org.hidde2727.DiscordPlugin.Discord.Discord;
import org.hidde2727.DiscordPlugin.Discord.Embed;
import org.hidde2727.DiscordPlugin.Storage.Config;
import org.hidde2727.DiscordPlugin.Storage.DataStorage;
import org.hidde2727.DiscordPlugin.Storage.Language;

import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class InfoCommand extends ListenerAdapter {
    DiscordPlugin plugin;
    Discord discord;
    Config.InfoCommand config;
    DataStorage permanentData;
    PlayerManager players;
    String commandName;
    String minecraftSubcommandName;
    String discordSubcommandName;
    String minecraftOptionName;
    String discordOptionName;

    public InfoCommand(DiscordPlugin plugin) {
        this.plugin = plugin;
        this.discord = plugin.discord;
        this.config = plugin.config.infoCommand;
        this.permanentData = plugin.dataStorage;
        this.players = plugin.players;

        if(!config.enabled) return;

        SlashCommandData command = BuildCommand();
        if(command == null) return;
        discord.AddCommand(command);
    }

    private SlashCommandData BuildCommand() {
        Language.Command cmdLanguage = plugin.language.commands.get("info");
        if(cmdLanguage == null) {
            Logs.error("Info command missing from the language.yml file");
            return null;
        }
        Language.Command.Option minecraftSubcommandLanguage = cmdLanguage.options.get("subcommandMinecraft");
        Language.Command.Option discordSubcommandLanguage = cmdLanguage.options.get("subcommandDiscord");
        Language.Command.Option minecraftOptionLanguage = cmdLanguage.options.get("minecraftUser");
        Language.Command.Option discordOptionLanguage = cmdLanguage.options.get("discordUser");
        if(minecraftSubcommandLanguage == null) {
            Logs.error("Info command subcommandMinecraft option missing from the language.yml file");
            return null;
        }
        if(discordSubcommandLanguage == null) {
            Logs.error("Info command subcommandDiscord option missing from the language.yml file");
            return null;
        }
        if(minecraftOptionLanguage == null) {
            Logs.error("Info command minecraftUser option missing from the language.yml file");
            return null;
        }
        if(discordOptionLanguage == null) {
            Logs.error("Info command discordUser option missing from the language.yml file");
            return null;
        }

        commandName = plugin.stringProcessor.GetString(cmdLanguage.name);
        minecraftSubcommandName = minecraftSubcommandLanguage.name;
        discordSubcommandName = discordSubcommandLanguage.name;
        minecraftOptionName = minecraftOptionLanguage.name;
        discordOptionName = discordOptionLanguage.name;

        Predicate<String> pattern = Pattern.compile("^[a-z-]+$").asMatchPredicate();
        if(!
                pattern.test(commandName) ||
                !pattern.test(minecraftSubcommandName) || !pattern.test(discordSubcommandName) ||
                !pattern.test(minecraftOptionName) || !pattern.test(discordOptionName)
        ) {
            Logs.warn("infoCommand.name and infoCommand.option.name in the messages.properties file may only contain lowercase letters and dashes (-)");
            config.enabled = false;
            return null;
        }

        SlashCommandData command =  Commands.slash(
            commandName,
            plugin.stringProcessor.GetString(cmdLanguage.description)
        );

        OptionData minecraftOption = new OptionData(OptionType.STRING, minecraftOptionName, minecraftOptionLanguage.description);
        for(DataStorage.Player player : permanentData.players.values()) {
            minecraftOption.addChoice(player.minecraftName, players.GetMinecraftKey(player));
        }

        if(players.ConnectAccounts()) {
            // Add a subcommand
            SubcommandData minecraftInfo = (new SubcommandData(minecraftSubcommandLanguage.name, minecraftSubcommandLanguage.description))
                    .addOptions(minecraftOption);

            OptionData discordOption = new OptionData(OptionType.USER, discordOptionName, discordOptionLanguage.description);

            SubcommandData discordInfo = (new SubcommandData(discordSubcommandLanguage.name, discordSubcommandLanguage.description))
                    .addOptions(discordOption);

            command.addSubcommands(minecraftInfo, discordInfo);
        } else {
            command.addOptions(minecraftOption);
        }

        return command;
    }

    Map<String, String> GetVariables(User user) {
        Map<String, String> variables = new HashMap<>();
        variables.put("DISCORD_NAME", user.getName());
        variables.put("DISCORD_GLOBAL_NAME", user.getGlobalName());
        variables.put("DISCORD_EFFECTIVE_NAME", user.getEffectiveName());
        variables.put("DISCORD_UUID", user.getId());
        return variables;
    }
    Map<String, String> GetVariables(DataStorage.Player player) {
        Map<String, String> variables = GetVariables(discord.GetUserByID(player.discordUUID));
        variables.put("PLAYER_NAME", player.minecraftName);
        variables.put("PLAYER_UUID", player.minecraftUUID);
        variables.put("PLAYER_KEY", players.GetMinecraftKey(player.minecraftName, player.minecraftUUID));
        return variables;
    }
    Map<String, String> GetVariables(DataStorage.Player.Punishment punishment) {
        Map<String, String> variables = new HashMap<>();
        variables.put("PUNISHMENT_NAME", punishment.punishmentName);
        variables.put("PUNISHMENT", punishment.punishment.toString());
        if(punishment.punishment == Config.Banning.PunishmentPicker.PunishmentType.PermBan) {
            variables.put("PUNISHMENT_UNTIL", "permanent");
        } else if (punishment.punishment == Config.Banning.PunishmentPicker.PunishmentType.Kick){
            variables.put("PUNISHMENT_UNTIL", (punishment.until.toEpochSecond() + 31557014135661600L) + "x");
        } else {
            variables.put("PUNISHMENT_UNTIL", punishment.until.truncatedTo(ChronoUnit.SECONDS).toString());
        }
        variables.put("PUNISHMENT_UNTIL_SECONDS", Integer.toString(punishment.until.getSecond()));
        variables.put("PUNISHMENT_UNTIL_MINUTES", Integer.toString(punishment.until.getMinute()));
        variables.put("PUNISHMENT_UNTIL_HOURS", Integer.toString(punishment.until.getHour()));
        variables.put("PUNISHMENT_UNTIL_DAY", Integer.toString(punishment.until.getDayOfMonth()));
        variables.put("PUNISHMENT_UNTIL_MONTH", Integer.toString(punishment.until.getMonthValue()));
        variables.put("PUNISHMENT_UNTIL_MONTH_FULL", punishment.until.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()));
        variables.put("PUNISHMENT_UNTIL_YEAR", Integer.toString(punishment.until.getYear()));
        return variables;
    }

    public void OnPlayerAdd() {
        if(!config.enabled) return;
        // Reregister the command:
        SlashCommandData command = BuildCommand();
        if(command == null) return;
        discord.AddCommand(command);
    }
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if(!config.enabled) return;
        if (!event.getName().equals(commandName)) return;

        if(config.checkChannel) {
            if(!config.allowedChannels.contains(event.getChannelId())) {
                discord.CreateEmbed()
                        .SetLanguageNamespace("infoCommand", "wrongChannel")
                        .SetVariables(GetVariables(event.getUser()))
                        .Send(event, true);
                return;
            }
        }
        if(config.checkRoles) {
            if(!discord.DoesUserHaveRole(event.getUser(), config.allowedRoles)) {
                discord.CreateEmbed()
                        .SetLanguageNamespace("infoCommand", "notAllowed")
                        .SetVariables(GetVariables(event.getUser()))
                        .Send(event, true);
                return;
            }
        }

        DataStorage.Player player;
        if(players.ConnectAccounts()) {
            // Check the subcommand
            if(Objects.equals(event.getSubcommandName(), minecraftSubcommandName)) {
                if(event.getOption(minecraftOptionName) == null)  { SendIncompleteMessage(event); return; }
                String minecraftKey = event.getOption(minecraftOptionName).getAsString();
                player = players.GetPlayer(minecraftKey);
            }
            else if(Objects.equals(event.getSubcommandName(), discordSubcommandName)) {
                if(event.getOption(discordOptionName) == null)  { SendIncompleteMessage(event); return; }
                User user = event.getOption(discordOptionName).getAsUser();
                player = players.GetPlayerByDiscord(user.getId());

                if(player == null) {
                    discord.CreateEmbed()
                            .SetLanguageNamespace("infoCommand", "notRegistered")
                            .SetVariables(GetVariables(user))
                            .Send(event, true);
                    return;
                }
            }
            else {
                Logs.warn("Received a slash command interaction with an illegal subcommand name '" + event.getSubcommandName() + "'");
                return;
            }
        } else {
            if(event.getOption(minecraftOptionName) == null) { SendIncompleteMessage(event); return; }
            String minecraftKey = event.getOption(minecraftOptionName).getAsString();
            player = players.GetPlayer(minecraftKey);
        }

        if(player == null) {
            Logs.warn("Could not find a player requested with the list command, that shouldn't be possible");
            return;
        }

        Embed embed = discord.CreateEmbed()
                .SetLanguageNamespace("infoCommand", "command")
                .SetVariables(GetVariables(player));
        for(DataStorage.Player.Punishment punishment : player.punishments) {
            StringProcessor.VariableMap variables = new StringProcessor.VariableMap();
            for(Map.Entry<String, String> entry : GetVariables(punishment).entrySet()) {
                variables.Add(entry.getKey(), entry.getValue());
            }
            StringProcessor processor = plugin.stringProcessor.AddVariables(variables, 150);
            embed.AddToTranslation("description", processor.ProcessVariables(embed.GetTranslation("extra")));
        }
        embed.Send(event, true);
    }

    private void SendIncompleteMessage(SlashCommandInteractionEvent event) {
        discord.CreateEmbed()
                .SetLanguageNamespace("infoCommand", "incomplete")
                .SetVariables(GetVariables(event.getUser()))
                .Send(event, true);
    }
}