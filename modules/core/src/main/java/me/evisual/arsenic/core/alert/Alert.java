package me.evisual.arsenic.core.alert;

import java.util.Objects;
import java.util.UUID;

public final class Alert {
    private final UUID playerId;
    private final String playerName;
    private final String checkName;
    private final AlertSeverity severity;
    private final int violationLevel;
    private final String detail;
    private final long timestampMillis;

    public Alert(UUID playerId,
                 String playerName,
                 String checkName,
                 AlertSeverity severity,
                 int violationLevel,
                 String detail,
                 long timestampMillis) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.playerName = Objects.requireNonNull(playerName, "playerName");
        this.checkName = Objects.requireNonNull(checkName, "checkName");
        this.severity = Objects.requireNonNull(severity, "severity");
        this.violationLevel = violationLevel;
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

    public AlertSeverity getSeverity() {
        return severity;
    }

    public int getViolationLevel() {
        return violationLevel;
    }

    public String getDetail() {
        return detail;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }
}
