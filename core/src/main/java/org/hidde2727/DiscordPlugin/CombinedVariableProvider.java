package org.hidde2727.DiscordPlugin;

import java.util.List;

public class CombinedVariableProvider implements VariableProvider {
    public CombinedVariableProvider(VariableProvider... providers) {
        this.providers = List.of(providers);
    }

    @Override
    public VariableMap getVariables() {
        VariableMap variables = new VariableMap();
        for(VariableProvider provider : providers) {
            variables.addAll(provider.getVariables());
        }
        return variables;
    }

    private final List<VariableProvider> providers;
}
