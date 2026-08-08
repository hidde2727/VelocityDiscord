package org.hidde2727.DiscordPlugin;

import java.awt.Color;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import net.dv8tion.jda.api.entities.emoji.Emoji;

public class StringProcessor implements Cloneable {
    private SortedMap<Integer, VariableMap> variables = new TreeMap<>();

    public StringProcessor(VariableMap variables) {
        this.variables.put(100, variables);
    }

    public static StringProcessor getDefault() {
        VariableMap map = new VariableMap();
        map.addFunction("CURRENT_DATE", () -> { return LocalDate.now().toString(); });
        map.addFunction("CURRENT_TIME", () -> { return LocalTime.now().truncatedTo(ChronoUnit.MINUTES).toString(); });
        map.addFunction("CURRENT_DATE_TIME", () -> { return LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).toString(); });
        map.addFunction("CURRENT_NANO_SECONDS", () -> { return String.valueOf(LocalDateTime.now().getNano()); });
        map.addFunction("CURRENT_SECOND", () -> { return String.valueOf(LocalDateTime.now().getSecond()); });
        map.addFunction("CURRENT_MINUTE", () -> { return String.valueOf(LocalDateTime.now().getMinute()); });
        map.addFunction("CURRENT_HOUR", () -> { return String.valueOf(LocalDateTime.now().getHour()); });
        map.addFunction("CURRENT_DAY", () -> { return String.valueOf(LocalDateTime.now().getDayOfMonth()); });
        map.addFunction("CURRENT_MONTH", () -> { return String.valueOf(LocalDateTime.now().getMonthValue()); });
        map.addFunction("CURRENT_YEAR", () -> { return String.valueOf(LocalDateTime.now().getYear()); });
        return new StringProcessor(map);
    }

    /**
     * Return a copy of this with the added variables
     * 
     * @param variables
     * @param priority 
     * @return Copy with variables added
     */
    public StringProcessor addVariables(VariableMap variables, int priority) {
        try {
            StringProcessor ret = (StringProcessor) this.clone();
            while(ret.variables.containsKey(priority)) {
                priority++;
            }
            ret.variables.put(priority, variables);
            return ret;
        } catch(Exception ignored) {
            return null;
        }
    }

    /**
     * Returns the variable associated with the key.
     * 
     * @param key
     * @return
     */
    public String getVariable(String key) {
        for (VariableMap variableMap : variables.values()) {
            if(variableMap.contains(key)) {
                return variableMap.get(key);
            }
        }
        return null;
    }

    /**
     * Tries to replace all the occurances of ${variableKey} with the correct variable string
     * If it cannot find the variableKey it will be ignored
     * 
     * @param str The string to process
     * @return The string with all the ${variableKey} subsituted
     */
    public String processVariables(String str) {
        if(str == null) return null;
        String ret = "";
        int currentOffset = 0;
        while(true) {
            int replacementIdx = str.indexOf("${", currentOffset);
            if(replacementIdx == -1) break;
            ret += str.substring(currentOffset, replacementIdx);
            currentOffset = replacementIdx;
            int endIdx = str.indexOf("}", replacementIdx);
            if(endIdx == -1) break;
            String variableKey = str.substring(replacementIdx+2, endIdx);
            String replacement = getVariable(variableKey);
            if(replacement == null) {
                Logs.warn("Found an occurrence of '${" + variableKey + "}' but could not find this variable");
                ret += str.substring(currentOffset, endIdx+1);
                currentOffset = endIdx+1;
                continue;
            }
            currentOffset = endIdx+1;
            // Replace with the locale string
            ret += replacement;
        }
        ret += str.substring(currentOffset);
        return ret;
    }

    public String getString(String str) {
        return processVariables(str);
    }

    public Color getColor(String str) {
        String colString = getString(str);
        if(colString == null) return null;
        String colStringNoWhitespace = colString.replaceAll("\\s+","");
        try {
            // First try the colors constants
            return (Color)Color.class.getField(colStringNoWhitespace).get(null);
        } catch(Exception ignored) { }
        try {
            int firstComma = colStringNoWhitespace.indexOf(',');
            if(firstComma != -1) {
                // Try RGB split with commas
                int r,g,b;
                r = Integer.parseInt(colStringNoWhitespace.substring(0, firstComma));
                int secondComma = colStringNoWhitespace.indexOf(',', firstComma+1);
                if(secondComma == -1) return null;// Give up
                g = Integer.parseInt(colStringNoWhitespace.substring(firstComma+1, secondComma));
                b = Integer.parseInt(colStringNoWhitespace.substring(secondComma+1));
                return new Color(r,g,b);
            }
            int firstSpace = colString.indexOf(' ');
            if(firstSpace != -1) {
                // Try RGB split with space
                int r,g,b;
                r = Integer.parseInt(colString.substring(0, firstSpace));
                int secondSpace = colString.indexOf(' ', firstSpace+1);
                if(secondSpace == -1) return null;// Give up
                g = Integer.parseInt(colString.substring(firstSpace+1, secondSpace));
                b = Integer.parseInt(colString.substring(secondSpace+1));
                return new Color(r,g,b);
            }
        } catch(Exception ignored) {}
        return null;
    }

    public Emoji getEmoji(String str) {
        String unprocessed = getString(str);
        try {
            return Emoji.fromFormatted(unprocessed);
        } catch(Exception ignored) {}
        return null;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        StringProcessor clone = (StringProcessor)super.clone();
        clone.variables = new TreeMap<>();
        for(int key : variables.keySet()) {
            clone.variables.put(key, variables.get(key));
        }
        return clone;
    }
}
