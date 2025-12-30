package me.evisual.arsenic.bukkit.trust;

import me.evisual.arsenic.core.storage.AlertLogEntry;
import me.evisual.arsenic.core.storage.AlertLogSort;
import me.evisual.arsenic.core.storage.ReportStore;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class TrustScoreService {
    private final JavaPlugin plugin;
    private final ReportStore reportStore;

    public TrustScoreService(JavaPlugin plugin, ReportStore reportStore) {
        this.plugin = plugin;
        this.reportStore = reportStore;
    }

    public int computeScore(UUID playerId) {
        int base = plugin.getConfig().getInt("trust-score.base", 100);
        int min = plugin.getConfig().getInt("trust-score.min", 0);
        int max = getMaxScore();
        int sampleSize = plugin.getConfig().getInt("trust-score.sample-size", 50);
        double halfLifeHours = plugin.getConfig().getDouble("trust-score.half-life-hours", 12.0);
        double vlScale = plugin.getConfig().getDouble("trust-score.vl-scale", 10.0);

        List<AlertLogEntry> alerts = Collections.emptyList();
        try {
            alerts = reportStore.listAlerts(playerId, AlertLogSort.MOST_RECENT, sampleSize, 0);
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to compute trust score: " + ex.getMessage());
        }

        double penalty = 0.0;
        long now = System.currentTimeMillis();
        for (AlertLogEntry entry : alerts) {
            double severityWeight = severityWeight(entry.getSeverity());
            double vlWeight = 1.0 + (entry.getViolationLevel() / Math.max(1.0, vlScale));
            double ageHours = Math.max(0.0, (now - entry.getTimestampMillis()) / 3600000.0);
            double recencyWeight = Math.pow(0.5, ageHours / Math.max(0.1, halfLifeHours));
            penalty += severityWeight * vlWeight * recencyWeight;
        }

        int score = (int) Math.round(base - penalty);
        if (score < min) {
            score = min;
        }
        if (score > max) {
            score = max;
        }
        return score;
    }

    public int getMaxScore() {
        return plugin.getConfig().getInt("trust-score.max", 100);
    }

    private double severityWeight(String severity) {
        if (severity == null) {
            return plugin.getConfig().getDouble("trust-score.severity-weights.unknown", 1.0);
        }
        String key = severity.toLowerCase();
        return plugin.getConfig().getDouble("trust-score.severity-weights." + key, 1.0);
    }
}
