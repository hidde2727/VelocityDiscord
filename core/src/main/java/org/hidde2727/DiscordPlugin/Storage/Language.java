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
    private static Language instance;
    public static boolean init(File languageFile) {
        if(!languageFile.exists()) {
            Logs.warn("Failed, the language file does not exist");
            return false;
        }
        var loaderoptions = new LoaderOptions();
        TagInspector taginspector =
                tag -> tag.getClassName().equals(Language.class.getName());
        loaderoptions.setTagInspector(taginspector);
        DumperOptions options = new DumperOptions();
        Representer representer = new Representer(options);
        representer.getPropertyUtils().setSkipMissingProperties(true);
        Yaml yaml = new Yaml(new Constructor(Language.class, loaderoptions), representer, options);

        try {
            instance = yaml.load(new FileInputStream(languageFile));
        } catch(Exception exc) {
            Logs.warn("Failed to parse the language file");
            Logs.warn(exc.getMessage());
            return false;
        }
        return true;
    }
    public static Language getInstance() {
        return instance;
    }

    public static class Action {
        public String label = null;
        public String value = null;
        public String placeholder = null;
        public String emoji = null;
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
        public String title = null;
        public String description = null;
        public String color = null;
        public String imageUrl = null;
        public String thumbnailUrl = null;
        public String url = null;
        public Footer footer = new Footer();
        public Author author = new Author();
        public Map<String, Action> actions = new HashMap<>();
        public String extra = null;

        /// Debug data, to trace back this translation:
        public transient String translationKey;
    }
    public static class Modal {
        public String title = null;
        public Map<String, Action> actions = new HashMap<>();

        /// Debug data, to trace back this translation:
        public transient String translationKey;
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
    public static class Embeds {
        public Map<String, Embed> embeds = new HashMap<>();
    }
    public static class Modals {
        public Map<String, Modal> modals = new HashMap<>();
    }

    public Map<String, Embeds> embeds = new HashMap<>();
    public Map<String, Modals> modals = new HashMap<>();
    public Map<String, Command> commands = new HashMap<>();

    public Embed getEmbed(String namespace, String key) {
        if(!embeds.containsKey(namespace)) {
            Logs.warn("The namespace '" + namespace + "' was requested, but not found");
            return null;
        }
        Embed embed = embeds.get(namespace).embeds.get(key);
        embed.translationKey = namespace + "." + key;
        return embed;
    }
    public Modal getModal(String namespace, String key) {
        if(!modals.containsKey(namespace)) {
            Logs.warn("The namespace '" + namespace + "' was requested, but not found");
            return null;
        }
        Modal modal = modals.get(namespace).modals.get(key);
        modal.translationKey = namespace + "." + key;
        return modal;
    }
}
