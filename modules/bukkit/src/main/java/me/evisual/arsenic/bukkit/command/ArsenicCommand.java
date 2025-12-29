package me.evisual.arsenic.bukkit.command;

import me.evisual.arsenic.bukkit.ArsenicPlugin;
import me.evisual.arsenic.core.alert.Alert;
import me.evisual.arsenic.core.alert.AlertSeverity;
import me.evisual.arsenic.core.storage.PlayerReport;
import me.evisual.arsenic.core.storage.ReportStore;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.UUID;

public final class ArsenicCommand implements CommandExecutor, TabCompleter {
    private final ArsenicPlugin plugin;
    private final ReportStore reportStore;
    private final me.evisual.arsenic.bukkit.gui.ReportGuiService reportGuiService;

    public ArsenicCommand(ArsenicPlugin plugin,
                          ReportStore reportStore,
                          me.evisual.arsenic.bukkit.gui.ReportGuiService reportGuiService) {
        this.plugin = plugin;
        this.reportStore = reportStore;
        this.reportGuiService = reportGuiService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /arsenic testalert | /arsenic report <player> | /arsenic clearlogs <player> | /arsenic clearall");
            return true;
        }

        if (args[0].equalsIgnoreCase("testalert")) {
            return handleTestAlert(sender);
        }
        if (args[0].equalsIgnoreCase("report")) {
            return handleReport(sender, args);
        }
        if (args[0].equalsIgnoreCase("reload")) {
            return handleReload(sender);
        }
        if (args[0].equalsIgnoreCase("clearlogs")) {
            return handleClearLogs(sender, args);
        }
        if (args[0].equalsIgnoreCase("clearall")) {
            return handleClearAll(sender);
        }

        sender.sendMessage(ChatColor.RED + "Unknown subcommand. Try /arsenic testalert | /arsenic report <player> | /arsenic reload | /arsenic clearlogs <player> | /arsenic clearall");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return suggestPrefix(args[0],
                    "testalert",
                    "report",
                    "reload",
                    "clearlogs",
                    "clearall");
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("report") || sub.equals("clearlogs")) {
                return plugin.getServer().getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        return java.util.Collections.emptyList();
    }

    private List<String> suggestPrefix(String input, String... options) {
        String lower = input.toLowerCase();
        java.util.List<String> matches = new java.util.ArrayList<>();
        for (String option : options) {
            if (option.startsWith(lower)) {
                matches.add(option);
            }
        }
        return matches;
    }

    private boolean handleTestAlert(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by a player.");
            return true;
        }

        Player player = (Player) sender;
        Alert alert = new Alert(
                player.getUniqueId(),
                player.getName(),
                "TestAlert",
                AlertSeverity.HIGH,
                1,
                "manual",
                System.currentTimeMillis()
        );
        plugin.getAlertService().publish(alert);
        sender.sendMessage(ChatColor.GREEN + "Test alert sent.");
        return true;
    }

    private boolean handleReport(CommandSender sender, String[] args) {
        if (!sender.hasPermission("arsenic.report")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /arsenic report <player>");
            return true;
        }

        OfflinePlayer target = resolvePlayer(args[1]);
        if (target == null || (target.getName() == null && target.getUniqueId() == null)) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }

        if (sender instanceof Player) {
            Player viewer = (Player) sender;
            reportGuiService.openReport(viewer, target.getUniqueId(), safeName(target));
            return true;
        }

        Optional<PlayerReport> report;
        try {
            report = reportStore.findReport(target.getUniqueId());
        } catch (Exception ex) {
            sender.sendMessage(ChatColor.RED + "Failed to load report. Check console.");
            plugin.getLogger().warning("Failed to load report: " + ex.getMessage());
            return true;
        }

        if (!report.isPresent()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", safeName(target));
            sender.sendMessage(plugin.getMessageService().format("reports.no-data", placeholders));
            return true;
        }

        PlayerReport data = report.get();
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", safeName(target));
        placeholders.put("total", Integer.toString(data.getTotalAlerts()));
        placeholders.put("time", formatTimestamp(data.getLastAlertMillis()));
        placeholders.put("check", data.getLastCheck());
        placeholders.put("detail", data.getLastDetail());
        placeholders.put("severity", data.getLastSeverity());

        sender.sendMessage(plugin.getMessageService().format("reports.header", placeholders));
        sender.sendMessage(plugin.getMessageService().format("reports.total-alerts", placeholders));
        sender.sendMessage(plugin.getMessageService().format("reports.last-alert", placeholders));
        sender.sendMessage(plugin.getMessageService().format("reports.last-check", placeholders));
        sender.sendMessage(plugin.getMessageService().format("reports.last-detail", placeholders));
        return true;
    }

    private boolean handleClearLogs(CommandSender sender, String[] args) {
        if (!sender.hasPermission("arsenic.clear.logs")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /arsenic clearlogs <player>");
            return true;
        }
        OfflinePlayer target = resolvePlayer(args[1]);
        if (target == null || target.getUniqueId() == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }
        try {
            reportStore.clearAlerts(target.getUniqueId());
            sender.sendMessage(ChatColor.GREEN + "Cleared logs for " + safeName(target) + ".");
        } catch (Exception ex) {
            sender.sendMessage(ChatColor.RED + "Failed to clear logs. Check console.");
            plugin.getLogger().warning("Failed to clear logs: " + ex.getMessage());
        }
        return true;
    }

    private boolean handleClearAll(CommandSender sender) {
        if (!sender.hasPermission("arsenic.clear.all")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }
        try {
            reportStore.clearAll();
            sender.sendMessage(ChatColor.GREEN + "Cleared all player data.");
        } catch (Exception ex) {
            sender.sendMessage(ChatColor.RED + "Failed to clear data. Check console.");
            plugin.getLogger().warning("Failed to clear all data: " + ex.getMessage());
        }
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("arsenic.command")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }
        plugin.reloadConfig();
        plugin.getMessageService().reload();
        sender.sendMessage(ChatColor.GREEN + "Arsenic reloaded.");
        return true;
    }

    private OfflinePlayer resolvePlayer(String input) {
        try {
            UUID uuid = UUID.fromString(input);
            return plugin.getServer().getOfflinePlayer(uuid);
        } catch (IllegalArgumentException ignored) {
            return plugin.getServer().getOfflinePlayer(input);
        }
    }

    private String safeName(OfflinePlayer player) {
        String name = player.getName();
        return name == null ? player.getUniqueId().toString() : name;
    }

    private String formatTimestamp(long timestamp) {
        if (timestamp <= 0L) {
            return "Never";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timestamp));
    }
}
