package me.evisual.arsenic.bukkit;

import me.evisual.arsenic.core.alert.Alert;
import me.evisual.arsenic.core.alert.AlertSink;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public final class AlertNotifier implements AlertSink {
    private final JavaPlugin plugin;
    private final ConfigService configService;
    private final MessageService messageService;

    public AlertNotifier(JavaPlugin plugin, ConfigService configService, MessageService messageService) {
        this.plugin = plugin;
        this.configService = configService;
        this.messageService = messageService;
    }

    @Override
    public void publish(Alert alert) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", alert.getPlayerName());
        placeholders.put("check", alert.getCheckName());
        placeholders.put("vl", Integer.toString(alert.getViolationLevel()));
        placeholders.put("detail", alert.getDetail());
        placeholders.put("severity", colorizeSeverity(alert.getSeverity().name()));

        String formatted = messageService.format("alerts.format", placeholders);
        if (configService.getBoolean("alerts.log-to-console")) {
            plugin.getLogger().info(ChatColor.stripColor(formatted));
        }
        if (configService.getBoolean("alerts.notify-staff")) {
            String permission = configService.getString("alerts.staff-permission");
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (permission.isEmpty() || player.hasPermission(permission)) {
                    player.sendMessage(formatted);
                }
            }
        }
    }

    private String colorizeSeverity(String severity) {
        if ("HIGH".equalsIgnoreCase(severity)) {
            return ChatColor.RED + severity;
        }
        if ("MEDIUM".equalsIgnoreCase(severity)) {
            return ChatColor.YELLOW + severity;
        }
        if ("LOW".equalsIgnoreCase(severity)) {
            return ChatColor.GOLD + severity;
        }
        if ("CRITICAL".equalsIgnoreCase(severity)) {
            return ChatColor.DARK_RED + severity;
        }
        return severity;
    }
}
