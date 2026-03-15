package org.hidde2727.DiscordPlugin.Discord;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hidde2727.DiscordPlugin.Logs;
import org.hidde2727.DiscordPlugin.StringProcessor;

import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.interactions.callbacks.IModalCallback;

public class Modal {
    
    private Discord discord;
    private StringProcessor processor;
    private StringProcessor.VariableMap variables = new StringProcessor.VariableMap();
    private Map<String, String> translations = null;
    private String modalID;
    private List<Object> items = new ArrayList<>();

    Modal(Discord discord, String id) {
        this.discord = discord;
        this.modalID = id;
    }

    private String ProcessString(String key) {
        return processor.GetString(translations.get(key));
    }

    public Modal SetLanguageNamespace(String namespace, String key) {
        this.translations = discord.GetLanguage().GetModal(namespace, key);
        if(translations.isEmpty()) {
            Logs.warn("A modal with translation keys ['" + namespace + "', '" + key + "'] requested but no translations were not found");
        }
        return this;
    }
    public Modal SetVariable(String key, String value) {
        variables.Add(key, value);
        return this;
    }
    public Modal SetVariables(Map<String, String> variables) {
        for(Map.Entry<String, String> entry : variables.entrySet()) {
            this.variables.Add(entry.getKey(), entry.getValue());
        }
        return this;
    }
    public Modal Add(SelectMenu row) {
        items.add(row);
        return this;
    }
    public Modal Add(TextField row) {
        items.add(row);
        return this;
    }

    private net.dv8tion.jda.api.modals.Modal Build() {
        if(translations == null) {
            Logs.error("You must set the translation namespace and key of a modal before building it");
            return null;
        }

        this.processor = discord.GetStringProcessor().AddVariables(variables, 50);

        String title = ProcessString("title");
        if(title == null) title = "EMpty modal, no data was provided";

        net.dv8tion.jda.api.modals.Modal.Builder modal = net.dv8tion.jda.api.modals.Modal.create(modalID, title);
        for(Object item : items) {
            if(item instanceof SelectMenu) {
                SelectMenu menu = (SelectMenu)item;
                modal.addComponents(Label.of(
                    menu.GetLabel(processor, translations),
                    (net.dv8tion.jda.api.components.selections.SelectMenu)menu.Build(processor, translations)
                ));
            } else {
                TextField field = (TextField)item;
                modal.addComponents(Label.of(
                    field.GetLabel(processor, translations),
                    field.Build(processor, translations)
                ));
            }
        }

        return modal.build();
    }
    public void Send(IModalCallback replyCallback) {
        try {
            replyCallback.replyModal(this.Build()).queue();
        } catch(Exception exc) {
            Logs.warn("Failed to send a modal: " + exc.getMessage());
        }
    }
}
