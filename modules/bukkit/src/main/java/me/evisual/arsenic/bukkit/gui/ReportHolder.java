package me.evisual.arsenic.bukkit.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public final class ReportHolder implements InventoryHolder {
    private final UUID targetId;
    private final String targetName;

    public ReportHolder(UUID targetId, String targetName) {
        this.targetId = targetId;
        this.targetName = targetName;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public String getTargetName() {
        return targetName;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
