package me.evisual.arsenic.bukkit;

import me.evisual.arsenic.core.message.MessageFormatter;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStreamReader;
import java.io.File;
import java.util.Collections;
import java.util.Map;

public final class MessageService {
    private final JavaPlugin plugin;
    private final File messageFile;
    private FileConfiguration messages;
    private FileConfiguration defaults;

    public MessageService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.messageFile = new File(plugin.getDataFolder(), "messages.yml");
        loadDefaults();
        reload();
    }

    private void loadDefaults() {
        try {
            if (plugin.getResource("messages.yml") == null) {
                defaults = new YamlConfiguration();
                return;
            }
            InputStreamReader reader = new InputStreamReader(plugin.getResource("messages.yml"));
            defaults = YamlConfiguration.loadConfiguration(reader);
        } catch (Exception ex) {
            defaults = new YamlConfiguration();
        }
    }

    public void reload() {
        if (!messageFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messageFile);
        try {
            if (defaults != null) {
                messages.setDefaults(defaults);
                messages.options().copyDefaults(true);
                messages.save(messageFile);
            }
        } catch (Exception ignored) {
            // Keep running even if defaults can't be merged.
        }
    }

    public String getRaw(String path) {
        String value = messages.getString(path);
        if (value == null || value.isEmpty()) {
            if (defaults != null) {
                return defaults.getString(path, "");
            }
            return "";
        }
        return value;
    }

    public String format(String path, Map<String, String> placeholders) {
        String template = getRaw(path);
        String withPlaceholders = MessageFormatter.applyPlaceholders(template, placeholders);
        return ChatColor.translateAlternateColorCodes('&', withPlaceholders);
    }

    public String format(String path) {
        return format(path, Collections.emptyMap());
    }
}
