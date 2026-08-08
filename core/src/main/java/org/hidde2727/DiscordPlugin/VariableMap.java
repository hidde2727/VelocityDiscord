package org.hidde2727.DiscordPlugin;


import java.util.HashMap;
import java.util.Map;

public class VariableMap {
    private final Map<String, Object> variables = new HashMap<>();
    public interface VariableFunction {
        public String getReplacement();
    }

    public void add(String key, String value) {
        variables.put(key, value);
    }
    public void addFunction(String key, VariableFunction value) {
        variables.put(key, value);
    }
    public void addAll(VariableMap values) { variables.putAll(values.variables); }

    public boolean contains(String key) {
        return variables.containsKey(key);
    }

    public String get(String key) {
        Object value = variables.get(key);
        if(value instanceof VariableFunction) {
            return ((VariableFunction)value).getReplacement();
        } else {
            return (String) value;
        }
    }
}
