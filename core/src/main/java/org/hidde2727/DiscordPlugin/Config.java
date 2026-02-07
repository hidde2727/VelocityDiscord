package org.hidde2727.DiscordPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.inspector.TagInspector;

public class Config {
    public static Config Load(File configFile) {
        var loaderoptions = new LoaderOptions();
        TagInspector taginspector =
                tag -> tag.getClassName().equals(Config.class.getName());
        loaderoptions.setTagInspector(taginspector);
        Yaml yaml = new Yaml(new Constructor(Config.class, loaderoptions));

        try {
            return yaml.load(new FileInputStream(configFile));
        } catch(Exception exc) {
            Logs.warn("Failed to parse the config file");
            Logs.warn(exc.getMessage());
            return new Config();
        }
    }

    public static class Events {
        public static class OnStart {
            public boolean enabled = false;
            public String channel = "";
        }
        public static class OnStop {
            public boolean enabled = false;
            public String channel = "";
        }
        public static class OnJoin {
            public boolean enabled = false;
            public String channel = "";
        }
        public static class OnLeave {
            public boolean enabled = false;
            public String channel = "";
        }
        public static class OnMessage {
            public boolean enabled = false;
            public boolean forwardCommands = false;
            public Map<String, String> channels = new HashMap<>();
        }
        public OnStart onStart = new OnStart();
        public OnStop onStop = new OnStop();
        public OnJoin onJoin = new OnJoin();
        public OnLeave onLeave = new OnLeave();
        public OnMessage onMessage = new OnMessage();
    }

    public static class Whitelist {
        public static class OnAccept {
            public boolean enabled = false;
            public String channel = "";
        };
        public static class OnDeny {
            public boolean enabled = false;
            public String channel = "";
        };
        public static class Request {
            public boolean enabled = false;
            public String channel = "";
            public boolean checkRoles = false;
            public List<String> allowedRoles = new ArrayList<>();
        }
        public static class Voting {
            public boolean enabled = false;
            public String channel = "";
            public boolean checkRoles = false;
            public List<String> allowedRoles = new ArrayList<>();
            public String acceptVotes = "50%";
            public String denyVotes = "1";
        }
        public boolean enabled = false;
        public OnAccept onAccept = new OnAccept();
        public OnDeny onDeny = new OnDeny();
        public Request request = new Request();
        public Voting voting = new Voting();
    }

    public static class Banning {
        public static class Request {
            public boolean enabled = false;
            public String channel = "";
            public boolean checkRoles = false;
            public List<String> allowedRoles = new ArrayList<>();
        }
        public static class Voting {
            public boolean enabled = false;
            public String channel = "";
            public boolean checkRoles = false;
            public List<String> allowedRoles = new ArrayList<>();
            public String acceptVotes = "50%";
            public String denyVotes = "1";
        }
        public boolean enabled = false;
        public Request request = new Request();
        public Voting voting = new Voting();
    }

    public String botToken = "";
    public Events events = new Events();
    public boolean useUUID = true;
    public boolean connectAccounts = true;
    public Whitelist whitelist = new Whitelist();
    public Banning banning = new Banning();
}
