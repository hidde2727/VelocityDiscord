package org.hidde2727.VelocityDiscordPlugin;

import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;

public class DataStorage {
    public static DataStorage Load(File dataFile) {
        if(!dataFile.exists()) return new DataStorage();
        Yaml yaml = new Yaml(new Constructor(DataStorage.class, new LoaderOptions()));
        DataStorage data = new DataStorage();
        try {
            data = (DataStorage) yaml.load(new FileInputStream(dataFile));
            return data;
        } catch(Exception exc) {
            Logs.logger.warn("Failed to parse the data storage");
            Logs.logger.warn(exc.getMessage());
            return new DataStorage();
        }
    }
    public void Unload(File dataFile) {
        Yaml yaml = new Yaml(new Constructor(DataStorage.class, new LoaderOptions()));
        try {
            FileWriter writer = new FileWriter(dataFile);
            yaml.dump(this, writer);
        } catch(Exception exc) {
            Logs.logger.warn("Failed to unload the data storage");
            Logs.logger.warn(exc.getMessage());
        }
    }

    public static class Whitelist {
        public static class Request {
            public Request() {}
            public Request(String discordUUID, String minecraftName) {
                this.discordUUID = discordUUID;
                this.minecraftName = minecraftName;
            }
            public String discordUUID;
            public String minecraftName;
            public List<String> upVotes = new ArrayList<>();// Discord user ids of people that upvoted
            public List<String> downVotes = new ArrayList<>();// Discord user ids of people that upvoted
            // Runtime data:
            public String channelID;
            public Long messageID;
        }
        // Minecraft name to request
        public Map<String, Request> requests = new HashMap<>();
        // Minecraft name, to discord uuid
        public Map<String, String> whitelisted = new HashMap<>();

        public void setRequests(Map<String, Request> requests) {
            this.requests = requests;
        }
        public Map<String, Request> getRequests() {
            return requests;
        }
        public void setWhitelisted(Map<String, String> whitelisted) {
            this.whitelisted = whitelisted;
        }
        public Map<String, String> getWhitelisted() {
            return whitelisted;
        }
    }
    public static class Bans {

    }

    Whitelist whitelist = new Whitelist();
    Bans bans = new Bans();

    public void setWhitelist(Whitelist whitelist) {
        this.whitelist = whitelist;
    }
    public Whitelist getWhitelist() {
        return whitelist;
    }
    public void setBans(Bans bans) {
        this.bans = bans;
    }
    public Bans getBans() {
        return bans;
    }
}
