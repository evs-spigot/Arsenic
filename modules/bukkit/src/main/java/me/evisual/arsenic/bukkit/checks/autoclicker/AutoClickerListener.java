package me.evisual.arsenic.bukkit.checks.autoclicker;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.entity.Player;

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
        service.handleClick(event.getPlayer(), System.currentTimeMillis());
    }

    @EventHandler(ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        Player damager = (Player) event.getDamager();
        service.handleClick(damager, System.currentTimeMillis());
    }
}
