package me.evisual.arsenic.bukkit.banwaves;

import me.evisual.arsenic.bukkit.MessageService;
import me.evisual.arsenic.bukkit.trust.TrustScoreService;
import me.evisual.arsenic.core.storage.ReportStore;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class BanWaveService {
    private final JavaPlugin plugin;
    private final ReportStore reportStore;
    private final TrustScoreService trustScoreService;
    private final MessageService messageService;
    private final Map<UUID, String> queue = new LinkedHashMap<>();
    private int taskId = -1;

    public BanWaveService(JavaPlugin plugin, ReportStore reportStore) {
        this.plugin = plugin;
        this.reportStore = reportStore;
        this.trustScoreService = new TrustScoreService(plugin, reportStore);
        this.messageService = new MessageService(plugin);
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("ban-waves.enabled")) {
            return;
        }
        long intervalMinutes = plugin.getConfig().getLong("ban-waves.interval-minutes", 30L);
        long intervalTicks = Math.max(1L, intervalMinutes) * 60L * 20L;
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::runWave, intervalTicks, intervalTicks);
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    public void consider(UUID playerId, String playerName) {
        if (!plugin.getConfig().getBoolean("ban-waves.enabled")) {
            return;
        }
        int threshold = plugin.getConfig().getInt("ban-waves.threshold", 50);
        int score = trustScoreService.computeScore(playerId);
        if (score <= threshold) {
            queue.put(playerId, playerName);
        }
    }

    private void runWave() {
        if (queue.isEmpty()) {
            if (plugin.getConfig().getBoolean("ban-waves.broadcast.enabled")) {
                Bukkit.broadcastMessage(messageService.format("ban-waves.none"));
            }
            return;
        }
        String commandTemplate = plugin.getConfig().getString("ban-waves.command", "/ban {player}");
        int threshold = plugin.getConfig().getInt("ban-waves.threshold", 50);
        boolean includeSkipped = plugin.getConfig().getBoolean("ban-waves.broadcast.include-skipped", true);

        int banned = 0;
        int skipped = 0;
        for (Map.Entry<UUID, String> entry : queue.entrySet()) {
            UUID playerId = entry.getKey();
            String name = resolveName(playerId, entry.getValue());
            int score = trustScoreService.computeScore(playerId);
            if (score <= threshold) {
                String command = commandTemplate.replace("{player}", name);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), stripSlash(command));
                banned++;
                if (plugin.getConfig().getBoolean("ban-waves.broadcast.enabled")) {
                    Bukkit.broadcastMessage(messageService.format("ban-waves.banned",
                            java.util.Collections.singletonMap("player", name)));
                }
            } else {
                skipped++;
                if (includeSkipped && plugin.getConfig().getBoolean("ban-waves.broadcast.enabled")) {
                    Bukkit.broadcastMessage(messageService.format("ban-waves.skipped",
                            java.util.Collections.singletonMap("player", name)));
                }
            }
        }
        queue.clear();

        if (plugin.getConfig().getBoolean("ban-waves.broadcast.enabled")) {
            java.util.Map<String, String> placeholders = new java.util.HashMap<>();
            placeholders.put("banned", Integer.toString(banned));
            placeholders.put("skipped", Integer.toString(skipped));
            Bukkit.broadcastMessage(messageService.format("ban-waves.summary", placeholders));
        }
    }

    private String resolveName(UUID playerId, String fallback) {
        OfflinePlayer offline = Bukkit.getOfflinePlayer(playerId);
        if (offline.getName() != null) {
            return offline.getName();
        }
        return fallback == null ? playerId.toString() : fallback;
    }

    private String stripSlash(String command) {
        if (command.startsWith("/")) {
            return command.substring(1);
        }
        return command;
    }
}
