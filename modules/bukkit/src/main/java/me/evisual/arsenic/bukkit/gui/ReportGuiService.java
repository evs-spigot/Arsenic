package me.evisual.arsenic.bukkit.gui;

import me.evisual.arsenic.bukkit.ArsenicPlugin;
import me.evisual.arsenic.core.storage.AlertLogEntry;
import me.evisual.arsenic.core.storage.AlertLogSort;
import me.evisual.arsenic.core.storage.PlayerReport;
import me.evisual.arsenic.core.storage.ReportStore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class ReportGuiService {
    public static final String REPORT_TITLE = ChatColor.DARK_RED + "Arsenic Report";
    public static final String LOGS_TITLE = ChatColor.DARK_RED + "Arsenic Logs";
    public static final String CONFIRM_TITLE = ChatColor.DARK_RED + "Confirm Action";

    private static final int LOGS_PER_PAGE = 45;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final ArsenicPlugin plugin;
    private final ReportStore reportStore;

    public ReportGuiService(ArsenicPlugin plugin, ReportStore reportStore) {
        this.plugin = plugin;
        this.reportStore = reportStore;
    }

    public void openReport(Player viewer, UUID targetId, String targetName) {
        Inventory inventory = Bukkit.createInventory(new ReportHolder(targetId, targetName), 54, REPORT_TITLE);
        fillReportInventory(inventory, viewer, targetId, targetName);
        viewer.openInventory(inventory);
    }

    public void openLogs(Player viewer, UUID targetId, String targetName, AlertLogSort sort, int page) {
        Inventory inventory = Bukkit.createInventory(new LogsHolder(targetId, targetName, sort, page), 54, LOGS_TITLE);
        fillLogsInventory(inventory, targetId, targetName, sort, page);
        viewer.openInventory(inventory);
    }

    public void refreshLogs(Player viewer, LogsHolder holder) {
        Inventory inventory = Bukkit.createInventory(holder, 54, LOGS_TITLE);
        fillLogsInventory(inventory, holder.getTargetId(), holder.getTargetName(), holder.getSort(), holder.getPage());
        viewer.openInventory(inventory);
    }

    public void refreshReport(Player viewer, ReportHolder holder) {
        Inventory inventory = Bukkit.createInventory(holder, 54, REPORT_TITLE);
        fillReportInventory(inventory, viewer, holder.getTargetId(), holder.getTargetName());
        viewer.openInventory(inventory);
    }

    public void openConfirm(Player viewer, UUID targetId, String targetName, ConfirmAction action) {
        Inventory inventory = Bukkit.createInventory(new ConfirmHolder(targetId, targetName, action), 27, CONFIRM_TITLE);
        fillConfirmInventory(inventory, targetId, targetName, action);
        viewer.openInventory(inventory);
    }

    private void fillReportInventory(Inventory inventory, Player viewer, UUID targetId, String targetName) {
        Optional<PlayerReport> report = Optional.empty();
        try {
            report = reportStore.findReport(targetId);
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to load report: " + ex.getMessage());
        }

        String firstJoin = formatFirstPlayed(targetId);
        String sessionStart = formatSessionStart(targetId);
        String sessionDuration = formatSessionDuration(targetId);

        inventory.setItem(10, buildPlayerHead(targetId, targetName));
        inventory.setItem(12, buildItem(Material.PAPER, ChatColor.GOLD + "Alerts Summary",
                lore(ChatColor.GRAY + "Total: " + ChatColor.WHITE + (report.isPresent() ? report.get().getTotalAlerts() : 0),
                        ChatColor.GRAY + "Last: " + ChatColor.WHITE + (report.isPresent() ? formatTimestamp(report.get().getLastAlertMillis()) : "Never"))));
        inventory.setItem(14, buildItem(Material.BOOK, ChatColor.YELLOW + "Last Check",
                lore(ChatColor.GRAY + "Check: " + ChatColor.WHITE + (report.isPresent() ? report.get().getLastCheck() : "None"))));
        inventory.setItem(15, buildItem(Material.NAME_TAG, ChatColor.YELLOW + "Last Detail",
                lore(ChatColor.GRAY + "Detail: " + ChatColor.WHITE + (report.isPresent() ? report.get().getLastDetail() : "None"))));
        inventory.setItem(16, buildItem(Material.REDSTONE, ChatColor.YELLOW + "Last Severity",
                lore(ChatColor.GRAY + "Severity: " + colorizeSeverity(report.isPresent() ? report.get().getLastSeverity() : "None"))));
        inventory.setItem(20, buildItem(resolveMaterial("CLOCK", "WATCH"), ChatColor.YELLOW + "First Joined",
                lore(ChatColor.GRAY + "Date: " + ChatColor.WHITE + firstJoin)));
        inventory.setItem(21, buildItem(resolveMaterial("COMPASS", "COMPASS"), ChatColor.YELLOW + "Session Start",
                lore(ChatColor.GRAY + "Time: " + ChatColor.WHITE + sessionStart)));
        inventory.setItem(22, buildItem(resolveMaterial("MAP", "MAP"), ChatColor.YELLOW + "Session Length",
                lore(ChatColor.GRAY + "Duration: " + ChatColor.WHITE + sessionDuration)));

        inventory.setItem(40, buildItem(Material.CHEST, ChatColor.RED + "Open Logs",
                lore(ChatColor.GRAY + "Click to view alert history")));

        if (viewer.hasPermission("arsenic.clear.logs")) {
            inventory.setItem(49, buildItem(Material.BARRIER, ChatColor.RED + "Clear Logs",
                    lore(ChatColor.GRAY + "Remove this player's alert history")));
        }
        if (viewer.hasPermission("arsenic.clear.all")) {
            inventory.setItem(50, buildItem(Material.TNT, ChatColor.DARK_RED + "Clear All Data",
                    lore(ChatColor.GRAY + "Remove all player data")));
        }

        fillEmptyWithFiller(inventory);
    }

    private void fillLogsInventory(Inventory inventory, UUID targetId, String targetName, AlertLogSort sort, int page) {
        List<AlertLogEntry> entries = new ArrayList<>();
        int total = 0;
        try {
            total = reportStore.countAlerts(targetId);
            int offset = Math.max(0, page) * LOGS_PER_PAGE;
            entries = reportStore.listAlerts(targetId, sort, LOGS_PER_PAGE, offset);
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to load alert logs: " + ex.getMessage());
        }

        int slot = 0;
        for (AlertLogEntry entry : entries) {
            if (slot >= LOGS_PER_PAGE) {
                break;
            }
            inventory.setItem(slot++, buildItem(Material.PAPER,
                    ChatColor.RED + entry.getCheckName() + ChatColor.GRAY + " VL" + entry.getViolationLevel(),
                    lore(
                            ChatColor.DARK_GRAY + "Player: " + ChatColor.WHITE + entry.getPlayerName(),
                            ChatColor.DARK_GRAY + "Severity: " + colorizeSeverity(entry.getSeverity()),
                            ChatColor.DARK_GRAY + "Detail: " + ChatColor.WHITE + entry.getDetail(),
                            ChatColor.DARK_GRAY + "Time: " + ChatColor.WHITE + formatTimestamp(entry.getTimestampMillis())
                    )));
        }

        inventory.setItem(45, buildItem(Material.ARROW, ChatColor.YELLOW + "Previous Page",
                lore(ChatColor.GRAY + "Page " + (page + 1))));
        inventory.setItem(47, buildItem(Material.BARRIER, ChatColor.RED + "Back to Report",
                lore(ChatColor.GRAY + "Return to summary")));
        inventory.setItem(49, buildItem(Material.HOPPER, ChatColor.GOLD + "Sort: " + formatSort(sort),
                lore(ChatColor.GRAY + "Click to change sorting")));
        inventory.setItem(53, buildItem(Material.ARROW, ChatColor.YELLOW + "Next Page",
                lore(ChatColor.GRAY + "Page " + (page + 1))));

        inventory.setItem(51, buildItem(Material.MAP, ChatColor.GRAY + "Log Count",
                lore(ChatColor.DARK_GRAY + "Total: " + ChatColor.WHITE + total,
                        ChatColor.DARK_GRAY + "Player: " + ChatColor.WHITE + targetName)));

        fillEmptyWithFiller(inventory);
    }

    private void fillConfirmInventory(Inventory inventory, UUID targetId, String targetName, ConfirmAction action) {
        String actionLabel = action == ConfirmAction.CLEAR_LOGS ? "Clear Logs" : "Clear All Data";
        inventory.setItem(11, buildWool((short) 5, ChatColor.GREEN + "Confirm " + actionLabel,
                lore(ChatColor.GRAY + "Target: " + ChatColor.WHITE + targetName)));
        inventory.setItem(15, buildWool((short) 14, ChatColor.RED + "Cancel",
                lore(ChatColor.GRAY + "Return to report")));
        inventory.setItem(13, buildItem(Material.PAPER, ChatColor.YELLOW + "Warning",
                lore(ChatColor.GRAY + "This cannot be undone")));

        fillEmptyWithFiller(inventory);
    }

    private ItemStack buildPlayerHead(UUID targetId, String targetName) {
        Material skullMaterial = Material.getMaterial("PLAYER_HEAD");
        if (skullMaterial == null) {
            skullMaterial = Material.getMaterial("SKULL_ITEM");
        }
        ItemStack item = new ItemStack(skullMaterial, 1, (short) 3);
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof SkullMeta) {
            SkullMeta skullMeta = (SkullMeta) meta;
            OfflinePlayer player = Bukkit.getOfflinePlayer(targetId);
            if (player != null && player.getName() != null) {
                skullMeta.setOwner(player.getName());
            } else {
                skullMeta.setOwner(targetName);
            }
            skullMeta.setDisplayName(ChatColor.RED + targetName);
            item.setItemMeta(skullMeta);
            return item;
        }
        meta.setDisplayName(ChatColor.RED + targetName);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildWool(short data, String name, List<String> lore) {
        String modernName = data == 5 ? "LIME_WOOL" : "RED_WOOL";
        Material modern = Material.getMaterial(modernName);
        if (modern != null) {
            return applyMeta(new ItemStack(modern), name, lore);
        }
        Material legacy = Material.getMaterial("WOOL");
        if (legacy == null) {
            legacy = Material.WOOL;
        }
        return applyMeta(new ItemStack(legacy, 1, data), name, lore);
    }

    private ItemStack buildGrayPane() {
        Material modern = Material.getMaterial("GRAY_STAINED_GLASS_PANE");
        if (modern != null) {
            return applyMeta(new ItemStack(modern), " ", null);
        }
        Material legacy = Material.getMaterial("STAINED_GLASS_PANE");
        if (legacy == null) {
            legacy = Material.STAINED_GLASS_PANE;
        }
        return applyMeta(new ItemStack(legacy, 1, (short) 7), " ", null);
    }

    private void fillEmptyWithFiller(Inventory inventory) {
        ItemStack filler = buildGrayPane();
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }

    private ItemStack applyMeta(ItemStack item, String name, List<String> lore) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private List<String> lore(String... lines) {
        List<String> lore = new ArrayList<>();
        for (String line : lines) {
            lore.add(line);
        }
        return lore;
    }

    private String formatTimestamp(long timestamp) {
        if (timestamp <= 0L) {
            return "Never";
        }
        return DATE_FORMAT.format(new Date(timestamp));
    }

    private String formatFirstPlayed(UUID targetId) {
        org.bukkit.OfflinePlayer offline = plugin.getServer().getOfflinePlayer(targetId);
        long firstPlayed = offline.getFirstPlayed();
        return firstPlayed <= 0L ? "Unknown" : DATE_FORMAT.format(new Date(firstPlayed));
    }

    private String formatSessionStart(UUID targetId) {
        me.evisual.arsenic.bukkit.session.SessionService sessionService = plugin.getSessionService();
        if (sessionService == null) {
            return "Offline";
        }
        Long start = sessionService.getSessionStart(targetId);
        if (start == null) {
            return "Offline";
        }
        return DATE_FORMAT.format(new Date(start));
    }

    private String formatSessionDuration(UUID targetId) {
        me.evisual.arsenic.bukkit.session.SessionService sessionService = plugin.getSessionService();
        if (sessionService == null) {
            return "Offline";
        }
        Long start = sessionService.getSessionStart(targetId);
        if (start == null) {
            return "Offline";
        }
        long duration = Math.max(0L, System.currentTimeMillis() - start);
        return formatDuration(duration);
    }

    private String formatDuration(long durationMs) {
        long seconds = durationMs / 1000L;
        long minutes = seconds / 60L;
        long hours = minutes / 60L;
        long days = hours / 24L;
        seconds %= 60L;
        minutes %= 60L;
        hours %= 24L;

        StringBuilder builder = new StringBuilder();
        if (days > 0) {
            builder.append(days).append("d ");
        }
        if (hours > 0) {
            builder.append(hours).append("h ");
        }
        if (minutes > 0) {
            builder.append(minutes).append("m ");
        }
        builder.append(seconds).append("s");
        return builder.toString().trim();
    }

    private Material resolveMaterial(String modern, String legacy) {
        Material material = Material.getMaterial(modern);
        if (material != null) {
            return material;
        }
        material = Material.getMaterial(legacy);
        if (material != null) {
            return material;
        }
        return Material.PAPER;
    }

    private String formatSort(AlertLogSort sort) {
        if (sort == AlertLogSort.HIGHEST_VL) {
            return "Highest VL";
        }
        if (sort == AlertLogSort.CHECK_NAME) {
            return "Alert Type";
        }
        return "Most Recent";
    }

    private String colorizeSeverity(String severity) {
        if (severity == null || severity.isEmpty()) {
            return ChatColor.WHITE + "None";
        }
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
        return ChatColor.WHITE + severity;
    }

    public void performConfirmAction(Player viewer, UUID targetId, String targetName, ConfirmAction action) {
        try {
            if (action == ConfirmAction.CLEAR_LOGS) {
                reportStore.clearAlerts(targetId);
                viewer.sendMessage(ChatColor.GREEN + "Cleared logs for " + targetName + ".");
            } else {
                reportStore.clearAll();
                viewer.sendMessage(ChatColor.GREEN + "Cleared all player data.");
            }
        } catch (Exception ex) {
            viewer.sendMessage(ChatColor.RED + "Failed to clear data. Check console.");
            plugin.getLogger().warning("Failed to clear data: " + ex.getMessage());
        }
    }
}
