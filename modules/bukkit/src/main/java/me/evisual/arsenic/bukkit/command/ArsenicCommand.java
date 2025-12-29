package me.evisual.arsenic.bukkit.command;

import me.evisual.arsenic.bukkit.ArsenicPlugin;
import me.evisual.arsenic.core.alert.Alert;
import me.evisual.arsenic.core.alert.AlertSeverity;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ArsenicCommand implements CommandExecutor {
    private final ArsenicPlugin plugin;

    public ArsenicCommand(ArsenicPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /arsenic testalert");
            return true;
        }

        if (!args[0].equalsIgnoreCase("testalert")) {
            sender.sendMessage(ChatColor.RED + "Unknown subcommand. Try /arsenic testalert");
            return true;
        }

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
}
