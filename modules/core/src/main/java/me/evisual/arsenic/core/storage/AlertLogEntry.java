package me.evisual.arsenic.core.storage;

import java.util.UUID;

public final class AlertLogEntry {
    private final UUID playerId;
    private final String playerName;
    private final String checkName;
    private final int violationLevel;
    private final String severity;
    private final String detail;
    private final long timestampMillis;

    public AlertLogEntry(UUID playerId,
                         String playerName,
                         String checkName,
                         int violationLevel,
                         String severity,
                         String detail,
                         long timestampMillis) {
        this.playerId = playerId;
        this.playerName = playerName == null ? "" : playerName;
        this.checkName = checkName == null ? "" : checkName;
        this.violationLevel = violationLevel;
        this.severity = severity == null ? "" : severity;
        this.detail = detail == null ? "" : detail;
        this.timestampMillis = timestampMillis;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getCheckName() {
        return checkName;
    }

    public int getViolationLevel() {
        return violationLevel;
    }

    public String getSeverity() {
        return severity;
    }

    public String getDetail() {
        return detail;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }
}
