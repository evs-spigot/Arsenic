package me.evisual.arsenic.core.storage;

import java.util.UUID;

public final class PlayerReport {
    private final UUID playerId;
    private final String playerName;
    private final int totalAlerts;
    private final long lastAlertMillis;
    private final String lastCheck;
    private final String lastDetail;
    private final String lastSeverity;

    public PlayerReport(UUID playerId,
                        String playerName,
                        int totalAlerts,
                        long lastAlertMillis,
                        String lastCheck,
                        String lastDetail,
                        String lastSeverity) {
        this.playerId = playerId;
        this.playerName = playerName == null ? "" : playerName;
        this.totalAlerts = totalAlerts;
        this.lastAlertMillis = lastAlertMillis;
        this.lastCheck = lastCheck == null ? "" : lastCheck;
        this.lastDetail = lastDetail == null ? "" : lastDetail;
        this.lastSeverity = lastSeverity == null ? "" : lastSeverity;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getTotalAlerts() {
        return totalAlerts;
    }

    public long getLastAlertMillis() {
        return lastAlertMillis;
    }

    public String getLastCheck() {
        return lastCheck;
    }

    public String getLastDetail() {
        return lastDetail;
    }

    public String getLastSeverity() {
        return lastSeverity;
    }
}
