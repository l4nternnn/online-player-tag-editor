package com.lantern.onlineplayertageditor.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lantern.onlineplayertageditor.OnlinePlayerTagEditor;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("online-player-tag-editor.json");

    private static ModConfig config;

    public static ModConfig getConfig() {
        if (config == null) {
            load();
        }
        return config;
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                config = GSON.fromJson(json, ModConfig.class);
                if (config == null) {
                    OnlinePlayerTagEditor.LOGGER.warn("Config file was empty or invalid, using defaults");
                    config = ModConfig.createDefault();
                    save();
                    return;
                }
                // Merge any new defaults not present in the saved config
                ModConfig defaults = ModConfig.createDefault();
                boolean changed = false;
                for (String tag : defaults.presetTags) {
                    if (!config.presetTags.contains(tag)) {
                        config.presetTags.add(tag);
                        changed = true;
                    }
                }
                for (var entry : defaults.tagDisplayNames.entrySet()) {
                    if (!config.tagDisplayNames.containsKey(entry.getKey())) {
                        config.tagDisplayNames.put(entry.getKey(), entry.getValue());
                        changed = true;
                    }
                }
                if (changed) {
                    save();
                    OnlinePlayerTagEditor.LOGGER.info("Config merged with new defaults");
                }
                OnlinePlayerTagEditor.LOGGER.info("Config loaded from {}", CONFIG_PATH);
            } catch (Exception e) {
                OnlinePlayerTagEditor.LOGGER.error("Failed to load config, using defaults", e);
                config = ModConfig.createDefault();
                save();
            }
        } else {
            config = ModConfig.createDefault();
            save();
            OnlinePlayerTagEditor.LOGGER.info("Created default config at {}", CONFIG_PATH);
        }
    }

    public static void reload() {
        OnlinePlayerTagEditor.LOGGER.info("Reloading config...");
        load();
        OnlinePlayerTagEditor.LOGGER.info("Config reloaded successfully");
    }

    private static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(config));
        } catch (IOException e) {
            OnlinePlayerTagEditor.LOGGER.error("Failed to save config", e);
        }
    }
}
