package org.hidde2727.DiscordPlugin.Flow.Voting;

import java.util.ArrayList;
import java.util.List;

public class Config {
    public boolean enabled = false;
    public String channel = "";
    public boolean checkRoles = false;
    public List<String> allowedRoles = new ArrayList<>();
    public String acceptVotes = "50%";
    public String denyVotes = "1";
}
