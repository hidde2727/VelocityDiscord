package org.hidde2727.DiscordPlugin.Models;

import org.hidde2727.DiscordPlugin.Logs;
import org.hidde2727.DiscordPlugin.VariableMap;
import org.hidde2727.DiscordPlugin.VariableProvider;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;

public class Punishment implements VariableProvider {
    public Punishment() {}
    private Punishment(Type punishment, String punishmentName, OffsetDateTime until, String reason) {
        this.punishment = punishment;
        this.punishmentName = punishmentName;
        this.until = until;
        this.reason = reason;
    }

    @Override
    public VariableMap getVariables() {
        VariableMap map = new VariableMap();
        map.add("PUNISHMENT_NAME", punishmentName);
        map.add("PUNISHMENT", punishment.toString());
        map.add("PUNISHMENT_UNTIL", until.truncatedTo(ChronoUnit.SECONDS).toString());
        map.add("PUNISHMENT_UNTIL_SECONDS", Integer.toString(until.getSecond()));
        map.add("PUNISHMENT_UNTIL_MINUTES", Integer.toString(until.getMinute()));
        map.add("PUNISHMENT_UNTIL_HOURS", Integer.toString(until.getHour()));
        map.add("PUNISHMENT_UNTIL_DAY", Integer.toString(until.getDayOfMonth()));
        map.add("PUNISHMENT_UNTIL_MONTH", Integer.toString(until.getMonthValue()));
        map.add("PUNISHMENT_UNTIL_MONTH_FULL", until.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()));
        map.add("PUNISHMENT_UNTIL_YEAR", Integer.toString(until.getYear()));
        return map;
    }



    /// Here for the snakeyaml serialization
    public String snakeyamlGetUntil() {
        try {
            return until.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch(Exception exc) {
            Logs.error("Failed to get the until property for snakeyaml because: " + exc.getMessage());
            return "ERROR_GETTING_ISO_OFFSET_TIME";
        }
    }
    /// Here for the snakeyaml serialization
    public void snakeyamlSetUntil(String str) {
        try {
            until = OffsetDateTime.parse(str, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch(Exception exc) {
            Logs.error("Failed to set until with snakeyaml because: " + exc.getMessage());
        }
    }

    public enum Type {
        Null,
        None,
        PermBan,
        Ban,
        Kick,
        NoUnban
        // Mute,
        // Warn,
    }
    public Type punishment = Type.Null;
    public String punishmentName = "";
    public transient OffsetDateTime until = OffsetDateTime.now();
    public String reason = "";
}
