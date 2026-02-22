package org.hidde2727.DiscordPlugin.Storage;

import org.hidde2727.DiscordPlugin.Logs;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.inspector.TagInspector;
import org.yaml.snakeyaml.representer.Representer;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Language {
    public static Language Load(File configFile) {
        var loaderoptions = new LoaderOptions();
        TagInspector taginspector =
                tag -> tag.getClassName().equals(Language.class.getName());
        loaderoptions.setTagInspector(taginspector);
        DumperOptions options = new DumperOptions();
        Representer representer = new Representer(options);
        representer.getPropertyUtils().setSkipMissingProperties(true);
        Yaml yaml = new Yaml(new Constructor(Language.class, loaderoptions), representer, options);

        try {
            return yaml.load(new FileInputStream(configFile));
        } catch(Exception exc) {
            Logs.warn("Failed to parse the language file");
            Logs.warn(exc.getMessage());
            return null;
        }
    }

    public static class Embed {
        public static class Footer {
            public String message = null;
            public String iconUrl = null;
        }
        public static class Author {
            public String name = null;
            public String url = null;
            public String iconUrl = null;
        }
        public static class Action {
            public String label = null;
            public String placeholder = null;
            public String emoji = null;
        }
        public String title = null;
        public String description = null;
        public String color = null;
        public String imageUrl = null;
        public String thumbnailUrl = null;
        public String url = null;
        public Footer footer = new Footer();
        public Author author = new Author();
        public Map<String, Action> actions = new HashMap<>();
    }
    public static class Modal {
        public static class Action {
            public String label = null;
            public String placeholder = null;
            public String value = null;
            public String emoji = null;
        }
        public String title = null;
        public Map<String, Action> actions = new HashMap<>();
    }
    public static class Command {
        public static class Option {
            public String name = "NO_NAME_SPECIFIED";
            public String description = null;
            public List<String> options = new ArrayList<>();
        }
        public String name = "NO_COMMAND_NAME_SPECIFIEID";
        public String description = null;
        public Map<String, Option> options = new HashMap<>();
    }
    public static class Defaults {
        public Embed embed= new Embed();
        public Modal modal = new Modal();
    }
    public static class Embeds {
        public Map<String, Embed> embeds = new HashMap<>();
    }
    public static class Modals {
        public Map<String, Modal> modals = new HashMap<>();
    }

    public Defaults defaults = new Defaults();
    public Map<String, Embeds> embeds = new HashMap<>();
    public Map<String, Modals> modals = new HashMap<>();
    public Map<String, Command> commands = new HashMap<>();

    public Map<String, String> GetEmbed(String namespace, String key) {
        Map<String, String> translations = new HashMap<>();
        if(!embeds.containsKey(namespace)) return new HashMap<>();
        if(!embeds.get(namespace).embeds.containsKey(key)) return new HashMap<>();
        Embed embed = embeds.get(namespace).embeds.get(key);
        translations.put("title", embed.title == null ? defaults.embed.title : embed.title);
        translations.put("description", embed.description == null ? defaults.embed.description : embed.description);
        translations.put("color", embed.color == null ? defaults.embed.color : embed.color);
        translations.put("imageUrl", embed.imageUrl == null ? defaults.embed.imageUrl : embed.imageUrl);
        translations.put("thumbnailUrl", embed.thumbnailUrl == null ? defaults.embed.thumbnailUrl : embed.thumbnailUrl);
        translations.put("url", embed.url == null ? defaults.embed.url : embed.url);
        translations.put("footer.message", embed.footer.message == null ? defaults.embed.footer.message : embed.footer.message);
        translations.put("footer.iconUrl", embed.footer.iconUrl == null ? defaults.embed.footer.iconUrl : embed.footer.iconUrl);
        translations.put("author.name", embed.author.name == null ? defaults.embed.author.name : embed.author.name);
        translations.put("author.url", embed.author.url == null ? defaults.embed.author.url : embed.author.url);
        translations.put("author.iconUrl", embed.author.iconUrl == null ? defaults.embed.author.iconUrl : embed.author.iconUrl);
        for(Map.Entry<String, Embed.Action> action : embed.actions.entrySet()) {
            translations.put("actions."+action.getKey()+".label", action.getValue().label);
            translations.put("actions."+action.getKey()+".placeholder", action.getValue().placeholder);
            translations.put("actions."+action.getKey()+".emoji", action.getValue().emoji);
        }
        return translations;
    }
    public Map<String, String> GetModal(String namespace, String key) {
        Map<String, String> translations = new HashMap<>();
        if(!modals.containsKey(namespace)) return new HashMap<>();
        if(!modals.get(namespace).modals.containsKey(key)) return new HashMap<>();
        Modal modal = modals.get(namespace).modals.get(key);
        translations.put("title", modal.title == null ? defaults.modal.title : modal.title);
        for(Map.Entry<String, Modal.Action> action : modal.actions.entrySet()) {
            translations.put("actions."+action.getKey()+".label", action.getValue().label);
            translations.put("actions."+action.getKey()+".placeholder", action.getValue().placeholder);
            translations.put("actions."+action.getKey()+".value", action.getValue().value);
            translations.put("actions."+action.getKey()+".emoji", action.getValue().emoji);
        }
        return translations;
    }
}
