package org.hidde2727.DiscordPlugin;

import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.inspector.TagInspector;
import org.yaml.snakeyaml.representer.Representer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.time.OffsetTime;
import java.util.ArrayList;
import java.util.HashMap;

public class DataStorage {
    public static DataStorage Load(File dataFile) {
        if(!dataFile.exists()) return new DataStorage();

        var loaderoptions = new LoaderOptions();
        TagInspector taginspector =
                tag -> tag.getClassName().equals(DataStorage.class.getName());
        loaderoptions.setTagInspector(taginspector);
        Yaml yaml = new Yaml(new Constructor(DataStorage.class, loaderoptions));

        try {
            return yaml.load(new FileInputStream(dataFile));
        } catch(Exception exc) {
            Logs.warn("Failed to parse the data storage");
            Logs.warn(exc.getMessage());
            return new DataStorage();
        }
    }
    public void Unload(File dataFile) {
        DumperOptions options = new DumperOptions();
        Representer representer = new Representer(options);
        representer.getPropertyUtils().setSkipMissingProperties(true);

        Yaml yaml = new Yaml(
                new Constructor(DataStorage.class, new LoaderOptions()),
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

    
    public static class Request {
        public Request() {}
        public Request(String discordUUID, String minecraftName, String minecraftUUID, String key) {
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
        public String channelID;
        public Long messageID;
    }
    public static class Bans {

    }
    public static class Player {
        public Player() {}
        public Player(String discordUUID, String minecraftName, String minecraftUUID) {
            this.discordUUID = discordUUID;
            this.minecraftName = minecraftName;
            this.minecraftUUID = minecraftUUID;
        }

        public boolean whitelisted = false;
        public boolean banned = false;
        public OffsetTime bannedTill;
        public String discordUUID;
        public String minecraftName;
        public String minecraftUUID;
    }

    // If the key off all the player maps is their name or UUID
    public boolean minecraftUUIDKey = false;
    // If one discord use is only allowed one minecraft account
    public boolean connectDiscord = false;

    public boolean inMaintenanceMode = false;

    public Map<String, Request> whitelistRequests = new HashMap<>();
    public Bans bans = new Bans();
    // Minecraft name/UUID, to discord uuid
    public Map<String, Player> players = new HashMap<>();
}
