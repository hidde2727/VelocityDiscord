package org.hidde2727.DiscordPlugin.Discord;

import java.util.ArrayList;
import java.util.List;

import org.hidde2727.DiscordPlugin.StringProcessor;

import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.interactions.callbacks.IModalCallback;

public class Modal {
    
    private Discord discord;
    private StringProcessor processor;
    private StringProcessor.VariableMap variables = new StringProcessor.VariableMap();
    private String modalID;
    private String namespace;
    private int maxNamespaceSearchDepth;
    private List<Object> items = new ArrayList<>();

    Modal(Discord discord, String id) {
        this.discord = discord;
        this.modalID = id;
    }

    private String ProcessString(String key) {
        return processor.GetString(key, namespace, maxNamespaceSearchDepth);
    }

    public Modal SetLocalizationNamespace(String namespace, int maxSearchDepth) {
        this.namespace = namespace;
        this.maxNamespaceSearchDepth = maxSearchDepth;
        return this;
    }
    public Modal SetVariable(String key, String value) {
        variables.Add(key, value);
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
        if(namespace == null) return null;

        this.processor = discord.GetStringProcessor().AddVariables(variables, 50);

        net.dv8tion.jda.api.modals.Modal.Builder modal = net.dv8tion.jda.api.modals.Modal.create(modalID, ProcessString("title"));
        for(Object item : items) {
            if(item instanceof SelectMenu) {
                SelectMenu menu = (SelectMenu)item;
                modal.addComponents(Label.of(
                    menu.GetLabel(processor, namespace, maxNamespaceSearchDepth),
                    (net.dv8tion.jda.api.components.selections.SelectMenu)menu.Build(processor, namespace, maxNamespaceSearchDepth)
                ));
            } else {
                TextField field = (TextField)item;
                modal.addComponents(Label.of(
                    field.GetLabel(processor, namespace, maxNamespaceSearchDepth),
                    field.Build(processor, namespace, maxNamespaceSearchDepth)
                ));
            }
        }

        return modal.build();
    }
    public void Send(IModalCallback replyCallback) {
        replyCallback.replyModal(this.Build()).queue();
    }
}
