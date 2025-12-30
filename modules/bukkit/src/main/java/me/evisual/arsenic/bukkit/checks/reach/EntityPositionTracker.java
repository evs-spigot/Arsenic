package me.evisual.arsenic.bukkit.checks.reach;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EntityPositionTracker {
    private static final int MAX_SAMPLES = 40;

    private final JavaPlugin plugin;
    private final Map<UUID, Deque<Sample>> history = new ConcurrentHashMap<>();
    private int taskId = -1;

    public EntityPositionTracker(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (taskId != -1) {
            return;
        }
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::capture, 1L, 1L);
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        history.clear();
    }

    public Sample getClosestSample(UUID entityId, long targetTime) {
        Deque<Sample> samples = history.get(entityId);
        if (samples == null || samples.isEmpty()) {
            return null;
        }
        Sample closest = null;
        long bestDelta = Long.MAX_VALUE;
        for (Sample sample : samples) {
            long delta = Math.abs(sample.timestamp - targetTime);
            if (delta < bestDelta) {
                bestDelta = delta;
                closest = sample;
            }
        }
        return closest;
    }

    private void capture() {
        long now = System.currentTimeMillis();
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof LivingEntity)) {
                    continue;
                }
                Location location = entity.getLocation();
                Deque<Sample> samples = history.computeIfAbsent(entity.getUniqueId(), id -> new ArrayDeque<>());
                samples.addLast(new Sample(location.getX(), location.getY(), location.getZ(), now));
                while (samples.size() > MAX_SAMPLES) {
                    samples.removeFirst();
                }
            }
        }
    }

    public static final class Sample {
        private final double x;
        private final double y;
        private final double z;
        private final long timestamp;

        public Sample(double x, double y, double z, long timestamp) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.timestamp = timestamp;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getZ() {
            return z;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}
