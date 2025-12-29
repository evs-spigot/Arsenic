package me.evisual.arsenic.bukkit;

import me.evisual.arsenic.core.message.MessageFormatter;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collections;
import java.util.Map;

public final class MessageService {
    private final JavaPlugin plugin;
    private final File messageFile;
    private FileConfiguration messages;

    public MessageService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.messageFile = new File(plugin.getDataFolder(), "messages.yml");
        reload();
    }

    public void reload() {
        if (!messageFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messageFile);
    }

    public String getRaw(String path) {
        return messages.getString(path, "");
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
