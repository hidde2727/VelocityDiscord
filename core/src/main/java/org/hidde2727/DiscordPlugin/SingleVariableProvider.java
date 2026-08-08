package org.hidde2727.DiscordPlugin;

public class SingleVariableProvider implements VariableProvider {
    public SingleVariableProvider(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public VariableMap getVariables() {
        VariableMap map = new VariableMap();
        map.add(key, value);
        return map;
    }

    String key;
    String value;
}
