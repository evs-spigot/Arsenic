package me.evisual.arsenic.bukkit.gui;

import me.evisual.arsenic.core.storage.AlertLogSort;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public final class LogsHolder implements InventoryHolder {
    private final UUID targetId;
    private final String targetName;
    private final AlertLogSort sort;
    private final int page;

    public LogsHolder(UUID targetId, String targetName, AlertLogSort sort, int page) {
        this.targetId = targetId;
        this.targetName = targetName;
        this.sort = sort;
        this.page = page;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public String getTargetName() {
        return targetName;
    }

    public AlertLogSort getSort() {
        return sort;
    }

    public int getPage() {
        return page;
    }

    public LogsHolder withSort(AlertLogSort newSort) {
        return new LogsHolder(targetId, targetName, newSort, page);
    }

    public LogsHolder withPage(int newPage) {
        return new LogsHolder(targetId, targetName, sort, newPage);
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
