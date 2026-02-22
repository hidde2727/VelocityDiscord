package org.hidde2727.DiscordPlugin.Storage;

import java.util.List;
import java.util.Map;

import org.hidde2727.DiscordPlugin.Logs;
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
import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;

import org.hidde2727.DiscordPlugin.Storage.Config.Banning.PunishmentPicker.PunishmentType;
import org.hidde2727.DiscordPlugin.Storage.DataStorage.Player.Punishment;

public class DataStorage {
    public static DataStorage Load(File dataFile) {
        if(!dataFile.exists()) return new DataStorage();

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
            DataStorage storage = yaml.load(new FileInputStream(dataFile));
            if(storage == null) return new DataStorage();
            return storage;
        } catch(Exception exc) {
            Logs.warn("Failed to parse the data storage");
            Logs.warn(exc.getMessage());
            return null;
        }
    }
    public void Unload(File dataFile) {
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

    
    public static class WhitelistRequest {
        public WhitelistRequest() {}
        public WhitelistRequest(String discordUUID, String minecraftName, String minecraftUUID, String key) {
            this.discordUUID = discordUUID;
            this.minecraftName = minecraftName;
            this.minecraftUUID = minecraftUUID;
            this.key = key;
        }
        public String key;// Either the minecraftUsername or minecraftUUID depending on the config
        public String discordUUID;
        public String minecraftName;
        public String minecraftUUID;
        public List<String> upVotes = new ArrayList<>();// Discord user ids of people that upvoted
        public List<String> downVotes = new ArrayList<>();// Discord user ids of people that upvoted
        // Runtime data:
        public transient String channelID;
        public transient Long messageID;
    }
    public static class BanRequest {
        public BanRequest() {}
        public BanRequest(String discordUUID, String originalReason, Player player, String minecraftKey) {
            this.suggestedByDiscordUUID = discordUUID;
            this.originalReason = originalReason;
            this.reason = originalReason;
            this.player = player;
            this.key = minecraftKey;
        }
        public String suggestedByDiscordUUID;
        public String originalReason;
        public Player player;
        public String key;
        public PunishmentType punishment = PunishmentType.Null;
        public String adminDiscordUUID;
        public String punishmentName;
        public String reason;
        public int duration;
        public List<String> upVotes = new ArrayList<>();// Discord user ids of people that upvoted
        public List<String> downVotes = new ArrayList<>();// Discord user ids of people that upvoted
        // Runtime data:
        public transient String channelID;
        public transient Long messageID;
    }
    public static class Player {
        public static class Punishment {
            public Punishment() {}
            public Punishment(PunishmentType punishment, String punishmentName, int duration, String reason) {
                this.punishment = punishment;
                this.punishmentName = punishmentName;
                if(punishment == PunishmentType.Kick) {
                    this.until = OffsetTime.MIN.plusSeconds(duration+1).minusNanos(1);
                } else {
                    this.until = OffsetTime.now().plusSeconds(duration);
                }
                this.reason = reason;
            }

            // These are for snakeyaml parsing
            public String snakeyamlGetUntil() { 
                try {
                    return until.format(DateTimeFormatter.ISO_OFFSET_TIME);
                } catch(Exception exc) {
                    Logs.error("Failed to get the until property for snakeyaml because: " + exc.getMessage());
                    return "ERROR_GETTING_ISO_OFFSET_TIME";
                }
            }
            public void snakeyamlSetUntil(String str) {
                try {
                    until = OffsetTime.parse(str, DateTimeFormatter.ISO_OFFSET_TIME);
                } catch(Exception exc) {
                    Logs.error("Failed to set until with snakeyaml because: " + exc.getMessage());
                }
            }

            public PunishmentType punishment = PunishmentType.Null;
            public String punishmentName = "";
            public transient OffsetTime until = OffsetTime.now();
            public String reason = "";
        }
        public Player() {}
        public Player(String discordUUID, String minecraftName, String minecraftUUID) {
            this.discordUUID = discordUUID;
            this.minecraftName = minecraftName;
            this.minecraftUUID = minecraftUUID;
        }

        public boolean whitelisted = false;
        public List<Punishment> punishments = new ArrayList<>();
        public String discordUUID;
        public String minecraftName;
        public String minecraftUUID;
    }
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

    public Map<String, WhitelistRequest> whitelistRequests = new HashMap<>();
    public Map<String, BanRequest> banRequests = new HashMap<>();
    // Requests where the punishment has been decided:
    public Map<String, BanRequest> banRequestsDecided = new HashMap<>();
    // Minecraft name/UUID, to registered player
    public Map<String, Player> players = new HashMap<>();
}
