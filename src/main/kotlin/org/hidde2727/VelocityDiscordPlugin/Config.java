package org.hidde2727.VelocityDiscordPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Config {

    class Events {
        class OnStart {
            boolean enabled = false;
            String channel = "";
        }
        class OnStop {
            boolean enabled = false;
            String channel = "";
        }
        class OnJoin {
            boolean enabled = false;
            String channel = "";
        }
        class OnLeave {
            boolean enabled = false;
            String channel = "";
        }
        class OnMessage {
            boolean enabled = false;
            boolean forwardCommands = false;
            Map<String, String> channels = new HashMap<>();
        }
        OnStart onStart = OnStart();
        OnStop onStop = OnStop();
        OnJoin onJoin = OnJoin();
        OnLeave onLeave = OnLeave();
        OnMessage onMessage = OnMessage();
    }

    class Whitelist {
        class Request {
            boolean enabled = false;
            String channel = "";
            boolean checkRoles = false;
            List<String> allowedRoles = new ArrayList<>();
        }
        class Voting {
            boolean enabled = false;
            String channel = "";
            boolean checkRoles = false;
            List<String> allowedRoles = new ArrayList<>();
            String acceptVotes = "50%";
            String denyVotes = "1";
        }
        boolean enabled = false;
        Request request = new Request();
        Voting voting = new Voting();
    }

    class Banning {
        class Request {
            boolean enabled = false;
            String channel = "";
            boolean checkRoles = false;
            List<String> allowedRoles = new ArrayList<>();
        }
        class Voting {
            boolean enabled = false;
            String channel = "";
            boolean checkRoles = false;
            List<String> allowedRoles = new ArrayList<>();
            String acceptVotes = "50%";
            String denyVotes = "1";
        }
        boolean enabled = false;
        Request request = new Request();
        Voting voting = new Voting();
    }

    String botToken = "";
    Events events = new Events();
    Whitelist whitelist = new Whitelist();
    Banning banning = new Banning();
}
