package me.evisual.arsenic.bukkit.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public final class ConfirmHolder implements InventoryHolder {
    private final UUID targetId;
    private final String targetName;
    private final ConfirmAction action;

    public ConfirmHolder(UUID targetId, String targetName, ConfirmAction action) {
        this.targetId = targetId;
        this.targetName = targetName;
        this.action = action;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public String getTargetName() {
        return targetName;
    }

    public ConfirmAction getAction() {
        return action;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
