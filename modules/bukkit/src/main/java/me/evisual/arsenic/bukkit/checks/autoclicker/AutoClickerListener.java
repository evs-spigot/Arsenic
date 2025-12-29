package me.evisual.arsenic.bukkit.checks.autoclicker;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;

public final class AutoClickerListener implements Listener {
    private final AutoClickerService service;

    public AutoClickerListener(AutoClickerService service) {
        this.service = service;
    }

    @EventHandler(ignoreCancelled = true)
    public void onAnimation(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) {
            return;
        }
        service.handleSwing(event.getPlayer());
    }
}
