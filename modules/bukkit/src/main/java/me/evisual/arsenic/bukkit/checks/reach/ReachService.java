package me.evisual.arsenic.bukkit.checks.reach;

import me.evisual.arsenic.bukkit.ConfigService;
import me.evisual.arsenic.core.alert.Alert;
import me.evisual.arsenic.core.alert.AlertSeverity;
import me.evisual.arsenic.core.alert.AlertService;
import me.evisual.arsenic.core.message.MessageFormatter;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ReachService {
    private final JavaPlugin plugin;
    private final ConfigService config;
    private final AlertService alertService;
    private final Map<UUID, Long> lastAlert = new ConcurrentHashMap<>();

    public ReachService(JavaPlugin plugin, ConfigService config, AlertService alertService) {
        this.plugin = plugin;
        this.config = config;
        this.alertService = alertService;
    }

    /** Reach Detection     *
     * This check functions by reconstructing the player's
     * attack and using ray-tracing to calculate the distance
     * between the point of origin from the player's attack
     * and the target where they were aiming. Some debounce
     * is included to avoid false positives. This debounce can
     * be tuned if false positives persist
     *
     * Flags if player's calculated reach is >= max-reach or max-reach-sprinting (if sprinting) + hitbox-expansion
     * @param attacker
     * @param target
     * @param sample
     * @param now
     */
    public void handleAttack(Player attacker, Entity target, EntityPositionTracker.Sample sample, long now) {
        if (!config.getBoolean("checks.reach.enabled")) {
            return;
        }
        if (target == null || sample == null) {
            return;
        }

        double baseReach = config.getConfig().getDouble("checks.reach.max-reach", 3.05);
        double sprintReach = config.getConfig().getDouble("checks.reach.max-reach-sprinting", 3.5);
        double maxReach = attacker.isSprinting() ? sprintReach : baseReach;
        double expansion = config.getConfig().getDouble("checks.reach.hitbox-expansion", 0.1);
        long cooldown = config.getConfig().getLong("checks.reach.cooldown-ms", 1500L);

        Location eye = attacker.getEyeLocation();
        ReachMath.Vec3 origin = new ReachMath.Vec3(eye.getX(), eye.getY(), eye.getZ());
        ReachMath.Vec3 direction = new ReachMath.Vec3(eye.getDirection().getX(),
                eye.getDirection().getY(),
                eye.getDirection().getZ()).normalize();

        ReachMath.Aabb box = createHitbox(sample, target, expansion);
        double t = ReachMath.rayIntersectAabb(origin, direction, box);
        if (t < 0.0) {
            if (config.getConfig().getBoolean("checks.reach.debug")) {
                plugin.getLogger().info("[Reach] No hitbox intersection for " + attacker.getName());
            }
            return;
        }
        double distance = t;
        if (config.getConfig().getBoolean("checks.reach.debug")) {
            plugin.getLogger().info("[Reach] " + attacker.getName() + " reach=" + formatDouble(distance)
                    + " max=" + formatDouble(maxReach));
        }
        if (distance > maxReach) {
            Long last = lastAlert.get(attacker.getUniqueId());
            if (last != null && now - last < cooldown) {
                return;
            }
            lastAlert.put(attacker.getUniqueId(), now);
            String name = config.getString("checks.reach.name", "Reach");
            String detailTemplate = config.getString("checks.reach.detail", "reach={reach} max={max}");
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("reach", formatDouble(distance));
            placeholders.put("max", formatDouble(maxReach));
            String detail = MessageFormatter.applyPlaceholders(detailTemplate, placeholders);
            alertService.publish(new Alert(attacker.getUniqueId(),
                    attacker.getName(),
                    name,
                    AlertSeverity.HIGH,
                    (int) Math.round(distance * 100.0),
                    detail,
                    now));
        }
    }

    private ReachMath.Aabb createHitbox(EntityPositionTracker.Sample sample, Entity target, double expansion) {
        double width = target instanceof Player ? 0.6 : 0.6;
        double height = target instanceof Player ? 1.8 : 1.8;

        double minX = sample.getX() - (width / 2.0) - expansion;
        double minY = sample.getY() - expansion;
        double minZ = sample.getZ() - (width / 2.0) - expansion;
        double maxX = sample.getX() + (width / 2.0) + expansion;
        double maxY = sample.getY() + height + expansion;
        double maxZ = sample.getZ() + (width / 2.0) + expansion;
        return new ReachMath.Aabb(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private String formatDouble(double value) {
        return String.format(java.util.Locale.US, "%.2f", value);
    }
}
