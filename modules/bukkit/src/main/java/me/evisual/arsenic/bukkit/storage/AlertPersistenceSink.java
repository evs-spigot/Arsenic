package me.evisual.arsenic.bukkit.storage;

import me.evisual.arsenic.core.alert.Alert;
import me.evisual.arsenic.core.alert.AlertSink;
import me.evisual.arsenic.core.storage.ReportStore;

import java.util.logging.Logger;

public final class AlertPersistenceSink implements AlertSink {
    private final ReportStore reportStore;
    private final Logger logger;

    public AlertPersistenceSink(ReportStore reportStore, Logger logger) {
        this.reportStore = reportStore;
        this.logger = logger;
    }

    @Override
    public void publish(Alert alert) {
        try {
            reportStore.recordAlert(alert);
        } catch (Exception ex) {
            // Keep alert flow alive even if storage fails.
            logger.warning("Failed to persist alert: " + ex.getMessage());
        }
    }
}
