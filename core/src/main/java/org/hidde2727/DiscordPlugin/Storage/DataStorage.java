package org.hidde2727.DiscordPlugin.Storage;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.hidde2727.DiscordPlugin.Logs;
import org.hidde2727.DiscordPlugin.Models.Player;
import org.hidde2727.DiscordPlugin.Models.Punishment;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.inspector.TagInspector;
import org.yaml.snakeyaml.representer.Representer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.HashMap;

public class DataStorage {
    private static DataStorage instance;
    public static DataStorage getInstance() {
        return instance;
    }
    public static boolean init(File dataFile) {
        if(!dataFile.exists()) {
            instance = new DataStorage();
            return true;
        }

        var loaderoptions = new LoaderOptions();
        TagInspector taginspector =
                tag -> tag.getClassName().equals(DataStorage.class.getName());
        loaderoptions.setTagInspector(taginspector);
        DumperOptions options = new DumperOptions();
        Representer representer = new Representer(options);
        // representer.getPropertyUtils().setSkipMissingProperties(true);

        Constructor constructor = new Constructor(DataStorage.class, loaderoptions);
        // Add the offsetTime type description for the punishment class:
        TypeDescription customTypeDescription = new TypeDescription(Punishment.class);
        customTypeDescription.substituteProperty("until", String.class, "snakeyamlGetUntil", "snakeyamlSetUntil");
        constructor.addTypeDescription(customTypeDescription);
        representer.addTypeDescription(customTypeDescription);

        Yaml yaml = new Yaml(constructor, representer, options);

        try {
            instance = yaml.load(new FileInputStream(dataFile));
            if(instance == null) instance = new DataStorage();
        } catch(Exception exc) {
            Logs.warn("Failed to parse the data storage");
            Logs.warn(exc.getMessage());
            return false;
        }
        return true;
    }
    public void storeToDisk(File dataFile) {
        DumperOptions options = new DumperOptions();
        Representer representer = new Representer(options);
        // representer.getPropertyUtils().setSkipMissingProperties(true);

        Constructor constructor = new Constructor(DataStorage.class, new LoaderOptions());
        // Add the offsetTime type description for the punishment class:
        TypeDescription customTypeDescription = new TypeDescription(Punishment.class);
        customTypeDescription.substituteProperty("until", String.class, "snakeyamlGetUntil", "snakeyamlSetUntil");
        constructor.addTypeDescription(customTypeDescription);
        representer.addTypeDescription(customTypeDescription);

        Yaml yaml = new Yaml(
                constructor,
                representer,
                options
        );
        try {
            FileWriter writer = new FileWriter(dataFile);
            yaml.dump(this, writer);
        } catch(Exception exc) {
            Logs.warn("Failed to unload the data storage");
            Logs.warn(exc.getMessage());
        }
    }

    public Player getPlayer(String playerKey) {
        return players.get(playerKey);
    }
    public Player getPlayer(String playerName, String playerUUID) {
        if(minecraftUUIDKey) return players.get(playerUUID);
        else return players.get(playerName);
    }

    
//    public static class WhitelistRequest {
//        public WhitelistRequest() {}
//        public WhitelistRequest(String discordUUID, String minecraftName, String minecraftUUID, String key) {
//            this.discordUUID = discordUUID;
//            this.minecraftName = minecraftName;
//            this.minecraftUUID = minecraftUUID;
//            this.key = key;
//        }
//        public String key;// Either the minecraftUsername or minecraftUUID depending on the config
//        public String discordUUID;
//        public String minecraftName;
//        public String minecraftUUID;
//        public List<String> upVotes = new ArrayList<>();// Discord user ids of people that upvoted
//        public List<String> downVotes = new ArrayList<>();// Discord user ids of people that upvoted
//        public MessageID messageID;
//    }
//    public static class BanRequest {
//        public BanRequest() {}
//        public BanRequest(String discordUUID, String originalReason, Player player, String minecraftKey) {
//            this.suggestedByDiscordUUID = discordUUID;
//            this.originalReason = originalReason;
//            this.reason = originalReason;
//            this.player = player;
//            this.key = minecraftKey;
//        }
//        public String suggestedByDiscordUUID;
//        public String originalReason;
//        public Player player;
//        public PunishmentType punishment = PunishmentType.Null;
//        public String adminDiscordUUID;
//        public String punishmentName;
//        public String reason;
//        public int duration;
//        public List<String> upVotes = new ArrayList<>();// Discord user ids of people that upvoted
//        public List<String> downVotes = new ArrayList<>();// Discord user ids of people that upvoted
//        public MessageID messageID;
//    }
//    public static class UnbanRequest {
//        public UnbanRequest() {}
//        public UnbanRequest(String byDiscordUUID, String reason, Player player, String minecraftKey, Punishment forPunishment) {
//            this.byDiscordUUID = byDiscordUUID;
//            this.reason = reason;
//            this.player = player;
//            this.key = minecraftKey;
//            this.forPunishment = forPunishment;
//        }
//        public String byDiscordUUID;
//        public String reason;
//        public Player player;
//        public Punishment forPunishment;
//        public String key;
//        public List<String> upVotes = new ArrayList<>();// Discord user ids of people that upvoted
//        public List<String> downVotes = new ArrayList<>();// Discord user ids of people that upvoted
//        public MessageID messageID;
//    }

    public static class Maintenance {
        public boolean configMaintenance = false;
        public boolean discordCommandMaintenance = false;

        public boolean InMaintenance() {
            return configMaintenance || discordCommandMaintenance;
        }
    }

    // If the key off all the player maps is their name or UUID
    public boolean minecraftUUIDKey = false;
    // If one discord use is only allowed one minecraft account
    public boolean connectDiscord = false;

    public Maintenance maintenance = new Maintenance();

//    public Map<String, WhitelistRequest> whitelistRequests = new HashMap<>();
//    public Map<String, BanRequest> banRequests = new HashMap<>();
//    // Requests where the punishment has been decided:
//    public Map<String, BanRequest> banRequestsDecided = new HashMap<>();
//    public Map<String, UnbanRequest> unbanRequests = new HashMap<>();
    // Minecraft name/UUID, to registered player
    public Map<String, Player> players = new HashMap<>();

    public boolean isBackup = false;
    public String storedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString();
}
