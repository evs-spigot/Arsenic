package me.evisual.arsenic.core.storage;

import me.evisual.arsenic.core.alert.Alert;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportStore {
    void init() throws Exception;

    void close() throws Exception;

    void recordAlert(Alert alert) throws Exception;

    Optional<PlayerReport> findReport(UUID playerId) throws Exception;

    List<AlertLogEntry> listAlerts(UUID playerId, AlertLogSort sort, int limit, int offset) throws Exception;

    int countAlerts(UUID playerId) throws Exception;

    void clearAlerts(UUID playerId) throws Exception;

    void clearAll() throws Exception;
}
