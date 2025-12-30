package me.evisual.arsenic.bukkit.checks.autoclicker;

import me.evisual.arsenic.bukkit.ConfigService;
import me.evisual.arsenic.core.alert.Alert;
import me.evisual.arsenic.core.alert.AlertSeverity;
import me.evisual.arsenic.core.alert.AlertService;
import me.evisual.arsenic.core.message.MessageFormatter;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AutoClickerService {
    private static final long WINDOW_MILLIS = 2000L;

    private final JavaPlugin plugin;
    private final ConfigService config;
    private final AlertService alertService;
    private final Map<UUID, ClickHistory> histories = new HashMap<>();

    public AutoClickerService(JavaPlugin plugin, ConfigService config, AlertService alertService) {
        this.plugin = plugin;
        this.config = config;
        this.alertService = alertService;
    }

    public void handleClick(Player player, long now, boolean ignoreBecauseDigging) {
        if (!config.getBoolean("checks.autoclicker.enabled")) {
            return;
        }
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        if (ignoreBecauseDigging) {
            return;
        }
        ClickHistory history = histories.computeIfAbsent(player.getUniqueId(), id -> new ClickHistory());
        history.addClick(now);

        if (config.getBoolean("checks.autoclicker.cps.enabled")) {
            evaluateCps(player, history, now);
        }
        if (config.getBoolean("checks.autoclicker.consistency.enabled")) {
            evaluateConsistency(player, history, now);
        }
        if (config.getBoolean("checks.autoclicker.pattern.enabled")) {
            evaluatePattern(player, history, now);
        }
        if (config.getBoolean("checks.autoclicker.tick-align.enabled")) {
            evaluateTickAlign(player, history, now);
        }
    }

    public void handleClick(Player player, long now) {
        handleClick(player, now, false);
    }

    private void evaluateCps(Player player, ClickHistory history, long now) {
        int cps = history.countClicksSince(now - 1000L);
        int threshold = config.getConfig().getInt("checks.autoclicker.cps.threshold", 15);
        long cooldown = config.getConfig().getLong("checks.autoclicker.cps.cooldown-ms", 1500L);
        if (cps >= threshold && history.canAlert("cps", now, cooldown)) {
            history.markAlert("cps", now);
            String name = config.getString("checks.autoclicker.cps.name", "Autoclicker CPS");
            String detailTemplate = config.getString("checks.autoclicker.cps.detail", "CPS={cps}");
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("cps", Integer.toString(cps));
            String detail = MessageFormatter.applyPlaceholders(detailTemplate, placeholders);
            alertService.publish(new Alert(player.getUniqueId(),
                    player.getName(),
                    name,
                    AlertSeverity.MEDIUM,
                    cps,
                    detail,
                    now));
        }
    }

    private void evaluateConsistency(Player player, ClickHistory history, long now) {
        int minSamples = config.getConfig().getInt("checks.autoclicker.consistency.min-samples", 12);
        double maxStdDev = config.getConfig().getDouble("checks.autoclicker.consistency.max-stddev-ms", 3.5);
        double minCps = config.getConfig().getDouble("checks.autoclicker.consistency.min-cps", 10.0);
        long cooldown = config.getConfig().getLong("checks.autoclicker.consistency.cooldown-ms", 2500L);

        if (!history.hasSamples(minSamples + 1)) {
            return;
        }
        double[] intervals = history.lastIntervals(minSamples);
        double mean = mean(intervals);
        if (mean <= 0.0) {
            return;
        }
        double cps = 1000.0 / mean;
        if (cps < minCps) {
            return;
        }
        double stddev = stddev(intervals, mean);
        if (stddev <= maxStdDev && history.canAlert("consistency", now, cooldown)) {
            history.markAlert("consistency", now);
            String name = config.getString("checks.autoclicker.consistency.name", "Autoclicker Consistency");
            String detailTemplate = config.getString("checks.autoclicker.consistency.detail", "avg={avg}ms std={std}ms cps={cps}");
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("avg", formatDouble(mean));
            placeholders.put("std", formatDouble(stddev));
            placeholders.put("cps", formatDouble(cps));
            String detail = MessageFormatter.applyPlaceholders(detailTemplate, placeholders);
            alertService.publish(new Alert(player.getUniqueId(),
                    player.getName(),
                    name,
                    AlertSeverity.LOW,
                    (int) Math.round(cps),
                    detail,
                    now));
        }
    }

    private void evaluatePattern(Player player, ClickHistory history, long now) {
        int minSamples = config.getConfig().getInt("checks.autoclicker.pattern.min-samples", 16);
        double minCps = config.getConfig().getDouble("checks.autoclicker.pattern.min-cps", 9.0);
        double maxModeRatio = config.getConfig().getDouble("checks.autoclicker.pattern.max-mode-ratio", 0.65);
        long cooldown = config.getConfig().getLong("checks.autoclicker.pattern.cooldown-ms", 3000L);

        if (!history.hasSamples(minSamples + 1)) {
            return;
        }
        double[] intervals = history.lastIntervals(minSamples);
        double mean = mean(intervals);
        if (mean <= 0.0) {
            return;
        }
        double cps = 1000.0 / mean;
        if (cps < minCps) {
            return;
        }
        double modeRatio = modeRatio(intervals);
        if (modeRatio >= maxModeRatio && history.canAlert("pattern", now, cooldown)) {
            history.markAlert("pattern", now);
            String name = config.getString("checks.autoclicker.pattern.name", "Autoclicker Pattern");
            String detailTemplate = config.getString("checks.autoclicker.pattern.detail", "mode={mode} cps={cps}");
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("mode", formatDouble(modeRatio * 100.0) + "%");
            placeholders.put("cps", formatDouble(cps));
            String detail = MessageFormatter.applyPlaceholders(detailTemplate, placeholders);
            alertService.publish(new Alert(player.getUniqueId(),
                    player.getName(),
                    name,
                    AlertSeverity.MEDIUM,
                    (int) Math.round(cps),
                    detail,
                    now));
        }
    }

    private void evaluateTickAlign(Player player, ClickHistory history, long now) {
        int minSamples = config.getConfig().getInt("checks.autoclicker.tick-align.min-samples", 20);
        double minCps = config.getConfig().getDouble("checks.autoclicker.tick-align.min-cps", 10.0);
        double minAlignedRatio = config.getConfig().getDouble("checks.autoclicker.tick-align.min-aligned-ratio", 0.8);
        int toleranceMs = config.getConfig().getInt("checks.autoclicker.tick-align.tolerance-ms", 3);
        long cooldown = config.getConfig().getLong("checks.autoclicker.tick-align.cooldown-ms", 3000L);

        if (!history.hasSamples(minSamples)) {
            return;
        }
        long[] clicks = history.lastClicks(minSamples);
        if (clicks.length < minSamples) {
            return;
        }
        double cps = computeCps(clicks);
        if (cps < minCps) {
            return;
        }
        double alignedRatio = alignedRatio(clicks, toleranceMs);
        if (alignedRatio >= minAlignedRatio && history.canAlert("tick-align", now, cooldown)) {
            history.markAlert("tick-align", now);
            String name = config.getString("checks.autoclicker.tick-align.name", "Autoclicker TickAlign");
            String detailTemplate = config.getString("checks.autoclicker.tick-align.detail", "aligned={aligned} cps={cps}");
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("aligned", formatDouble(alignedRatio * 100.0) + "%");
            placeholders.put("cps", formatDouble(cps));
            String detail = MessageFormatter.applyPlaceholders(detailTemplate, placeholders);
            alertService.publish(new Alert(player.getUniqueId(),
                    player.getName(),
                    name,
                    AlertSeverity.LOW,
                    (int) Math.round(cps),
                    detail,
                    now));
        }
    }

    private double mean(double[] values) {
        double sum = 0.0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.length;
    }

    private double stddev(double[] values, double mean) {
        double sum = 0.0;
        for (double value : values) {
            double diff = value - mean;
            sum += diff * diff;
        }
        return Math.sqrt(sum / values.length);
    }

    private double modeRatio(double[] values) {
        Map<Long, Integer> counts = new HashMap<>();
        int max = 0;
        for (double value : values) {
            long rounded = Math.round(value);
            int count = counts.getOrDefault(rounded, 0) + 1;
            counts.put(rounded, count);
            if (count > max) {
                max = count;
            }
        }
        if (values.length == 0) {
            return 0.0;
        }
        return (double) max / (double) values.length;
    }

    private double alignedRatio(long[] clicks, int toleranceMs) {
        int aligned = 0;
        for (long click : clicks) {
            long mod = click % 50L;
            long distance = Math.min(mod, 50L - mod);
            if (distance <= toleranceMs) {
                aligned++;
            }
        }
        return clicks.length == 0 ? 0.0 : (double) aligned / (double) clicks.length;
    }

    private double computeCps(long[] clicks) {
        if (clicks.length < 2) {
            return 0.0;
        }
        long first = clicks[0];
        long last = clicks[clicks.length - 1];
        long span = Math.max(1L, last - first);
        return (clicks.length - 1) * 1000.0 / span;
    }

    private String formatDouble(double value) {
        return String.format(java.util.Locale.US, "%.2f", value);
    }

    private static final class ClickHistory {
        private final Deque<Long> clicks = new ArrayDeque<>();
        private final Map<String, Long> lastAlert = new HashMap<>();

        void addClick(long timestamp) {
            clicks.addLast(timestamp);
            long cutoff = timestamp - WINDOW_MILLIS;
            while (!clicks.isEmpty() && clicks.peekFirst() < cutoff) {
                clicks.removeFirst();
            }
        }

        int countClicksSince(long cutoff) {
            int count = 0;
            for (Long time : clicks) {
                if (time >= cutoff) {
                    count++;
                }
            }
            return count;
        }

        boolean hasSamples(int count) {
            return clicks.size() >= count;
        }

        double[] lastIntervals(int count) {
            double[] intervals = new double[count];
            Object[] arr = clicks.toArray();
            int start = arr.length - (count + 1);
            for (int i = 0; i < count; i++) {
                long previous = (Long) arr[start + i];
                long current = (Long) arr[start + i + 1];
                intervals[i] = Math.max(1L, current - previous);
            }
            return intervals;
        }

        long[] lastClicks(int count) {
            int available = clicks.size();
            int start = Math.max(0, available - count);
            long[] result = new long[Math.min(count, available)];
            Object[] arr = clicks.toArray();
            int index = 0;
            for (int i = start; i < arr.length; i++) {
                result[index++] = (Long) arr[i];
            }
            return result;
        }

        boolean canAlert(String key, long now, long cooldownMs) {
            Long last = lastAlert.get(key);
            return last == null || now - last >= cooldownMs;
        }

        void markAlert(String key, long now) {
            lastAlert.put(key, now);
        }
    }
}
