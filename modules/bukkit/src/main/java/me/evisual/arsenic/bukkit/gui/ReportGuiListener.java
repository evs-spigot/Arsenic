package me.evisual.arsenic.bukkit.gui;

import me.evisual.arsenic.core.storage.AlertLogSort;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class ReportGuiListener implements Listener {
    private final ReportGuiService guiService;

    public ReportGuiListener(ReportGuiService guiService) {
        this.guiService = guiService;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        if (event.getInventory() == null || event.getCurrentItem() == null) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        if (event.getInventory().getHolder() instanceof ReportHolder) {
            event.setCancelled(true);
            ReportHolder holder = (ReportHolder) event.getInventory().getHolder();
            int slot = event.getRawSlot();
            if (slot == 40) {
                guiService.openLogs(player, holder.getTargetId(), holder.getTargetName(), AlertLogSort.MOST_RECENT, 0);
            } else if (slot == 49 && player.hasPermission("arsenic.clear.logs")) {
                guiService.openConfirm(player, holder.getTargetId(), holder.getTargetName(), ConfirmAction.CLEAR_LOGS);
            } else if (slot == 50 && player.hasPermission("arsenic.clear.all")) {
                guiService.openConfirm(player, holder.getTargetId(), holder.getTargetName(), ConfirmAction.CLEAR_ALL);
            }
        } else if (event.getInventory().getHolder() instanceof LogsHolder) {
            event.setCancelled(true);
            LogsHolder holder = (LogsHolder) event.getInventory().getHolder();
            int slot = event.getRawSlot();
            if (slot == 45) {
                int newPage = Math.max(0, holder.getPage() - 1);
                guiService.refreshLogs(player, holder.withPage(newPage));
            } else if (slot == 47) {
                guiService.openReport(player, holder.getTargetId(), holder.getTargetName());
            } else if (slot == 49) {
                guiService.refreshLogs(player, holder.withSort(nextSort(holder.getSort())));
            } else if (slot == 53) {
                guiService.refreshLogs(player, holder.withPage(holder.getPage() + 1));
            } else {
                player.sendMessage(ChatColor.GRAY + "Click the controls below to sort or page logs.");
            }
        } else if (event.getInventory().getHolder() instanceof ConfirmHolder) {
            event.setCancelled(true);
            ConfirmHolder holder = (ConfirmHolder) event.getInventory().getHolder();
            int slot = event.getRawSlot();
            if (slot == 11) {
                if (holder.getAction() == ConfirmAction.CLEAR_LOGS && player.hasPermission("arsenic.clear.logs")) {
                    guiService.performConfirmAction(player, holder.getTargetId(), holder.getTargetName(), holder.getAction());
                    guiService.openReport(player, holder.getTargetId(), holder.getTargetName());
                } else if (holder.getAction() == ConfirmAction.CLEAR_ALL && player.hasPermission("arsenic.clear.all")) {
                    guiService.performConfirmAction(player, holder.getTargetId(), holder.getTargetName(), holder.getAction());
                    guiService.openReport(player, holder.getTargetId(), holder.getTargetName());
                } else {
                    player.sendMessage(ChatColor.RED + "You do not have permission to do that.");
                    guiService.openReport(player, holder.getTargetId(), holder.getTargetName());
                }
            } else if (slot == 15) {
                guiService.openReport(player, holder.getTargetId(), holder.getTargetName());
            }
        }
    }

    private AlertLogSort nextSort(AlertLogSort current) {
        if (current == AlertLogSort.MOST_RECENT) {
            return AlertLogSort.HIGHEST_VL;
        }
        if (current == AlertLogSort.HIGHEST_VL) {
            return AlertLogSort.CHECK_NAME;
        }
        return AlertLogSort.MOST_RECENT;
    }
}
