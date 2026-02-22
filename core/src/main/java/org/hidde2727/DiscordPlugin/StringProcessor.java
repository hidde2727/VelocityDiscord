package org.hidde2727.DiscordPlugin;

import java.awt.Color;
import java.util.*;

import net.dv8tion.jda.api.entities.emoji.Emoji;

public class StringProcessor implements Cloneable {
    private SortedMap<Integer, VariableMap> variables = new TreeMap<>();

    public static class VariableMap {
        private final Map<String, Object> variables = new HashMap<>();
        public interface VariableFunction {
            public String getReplacement();
        }

        public void Add(String key, String value) {
            variables.put(key, value);
        }
        public void AddFunction(String key, VariableFunction value) {
            variables.put(key, value);
        }

        public boolean Contains(String key) {
            return variables.containsKey(key);
        }

        public String Get(String key) {
            Object value = variables.get(key);
            if(value instanceof VariableFunction) {
                return ((VariableFunction)value).getReplacement();
            } else {
                return (String) value;
            }
        }
    }


    public StringProcessor(VariableMap variables) {
        this.variables.put(100, variables);
    }

    /**
     * Return a copy of this with the added variables
     * 
     * @param variables
     * @param priority 
     * @return Copy with variables added
     */
    public StringProcessor AddVariables(VariableMap variables, int priority) {
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
     * Returns the correct variable
     * 
     * @param key
     * @return
     */
    public String GetVariable(String key) {
        for (VariableMap variableMap : variables.values()) {
            if(variableMap.Contains(key)) {
                return variableMap.Get(key);
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
    public String ProcessVariables(String str) {
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
            String replacement = GetVariable(variableKey);
            if(replacement == null) {
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

    public String GetString(String str) {
        return ProcessVariables(str);
    }

    public Color GetColor(String str) {
        String colString = GetString(str);
        if(colString == null) return null;
        String colStringNoWhitespace = colString.replaceAll("\\s+","");
        try {
            // First try the colors constants
            Color color = (Color)Color.class.getField(colStringNoWhitespace).get(null);
            return color;
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

    public Emoji GetEmoji(String str) {
        String unprocessed = GetString(str);
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
