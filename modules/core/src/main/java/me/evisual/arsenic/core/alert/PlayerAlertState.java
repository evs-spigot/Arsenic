package me.evisual.arsenic.core.alert;

import java.util.concurrent.atomic.AtomicInteger;

public final class PlayerAlertState {
    private final AtomicInteger totalAlerts = new AtomicInteger();
    private volatile long lastAlertMillis;

    public int incrementAndGet() {
        return totalAlerts.incrementAndGet();
    }

    public int getTotalAlerts() {
        return totalAlerts.get();
    }

    public long getLastAlertMillis() {
        return lastAlertMillis;
    }

    public void setLastAlertMillis(long lastAlertMillis) {
        this.lastAlertMillis = lastAlertMillis;
    }
}
