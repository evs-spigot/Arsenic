package me.evisual.arsenic.bukkit;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class ConfigService {
    private final JavaPlugin plugin;

    public ConfigService(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
    }

    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    public boolean getBoolean(String path) {
        return getConfig().getBoolean(path);
    }

    public String getString(String path) {
        return getConfig().getString(path, "");
    }

    public String getString(String path, String defaultValue) {
        String value = getConfig().getString(path);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value;
    }
}
