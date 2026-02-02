package org.hidde2727.VelocityDiscordPlugin;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.SortedMap;
import java.util.TreeMap;

public class StringProcessor implements Cloneable {
    private SortedMap<Integer, VariableMap> variables = new TreeMap<>();
    private ResourceBundle localization;

    public static class VariableMap {
        private Map<String, Object> variables = new HashMap<>();
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


    public StringProcessor(VariableMap variables, int priority, ResourceBundle localization) {
        this.variables.put(priority, variables);
        this.localization = localization;
    }
    public StringProcessor(VariableMap variables, ResourceBundle localization) {
        this.variables.put(100, variables);
        this.localization = localization;
    }
    /**
     * 
     * @param variables Prioritized map of variable maps
     * @param localization The localization bundle to use
     */
    public StringProcessor(SortedMap<Integer, VariableMap> variables, ResourceBundle localization) {
        this.variables = variables;
        this.localization = localization;
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

    /**
     * The following example:
     * ```
     * GetString("title.name", "embeds.events.OnStart", 3);
     * ```
     * Checks to find title.name in the following locations:
     * - embeds.events.OnStart.title.name
     * - embeds.events.title.name
     * - embeds.title.name
     * 
     * If the string was found, it will escape all the variables present (signalled by $(VARIABLE_NAME))
     * 
     * @param key The key
     * @param namespace The namespace
     * @param maxSearchDepth The maximum amount of namespace to check
     * @return null if not found, else the string
     */
    public String GetString(String key, String namespace, int maxSearchDepth) {
        // Find the string
        for(int i = 0; i < maxSearchDepth; i++) {
            if(localization.containsKey(namespace + "." + key)) break;
            int index = namespace.lastIndexOf('.');
            if(index == -1) return null;
            namespace = namespace.substring(0, index);
        }
        return ProcessVariables(localization.getString(namespace + "." + key));
    }

    public Color GetColor(String key, String namespace, int maxSearchDepth) {
        String colString = GetString(key, namespace, maxSearchDepth);
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

    @Override
    public Object clone() throws CloneNotSupportedException {
        StringProcessor clone = (StringProcessor)super.clone();
        clone.localization = this.localization;
        clone.variables = new TreeMap<>();
        for(int key : variables.keySet()) {
            clone.variables.put(key, variables.get(key));
        }
        return clone;
    }
}
