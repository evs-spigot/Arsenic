package me.evisual.arsenic.core.alert;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class AlertService {
    private final Map<UUID, PlayerAlertState> alertStates = new ConcurrentHashMap<>();
    private final List<AlertSink> sinks = new CopyOnWriteArrayList<>();

    public void registerSink(AlertSink sink) {
        sinks.add(Objects.requireNonNull(sink, "sink"));
    }

    public void unregisterSink(AlertSink sink) {
        sinks.remove(sink);
    }

    public PlayerAlertState getState(UUID playerId) {
        return alertStates.computeIfAbsent(playerId, id -> new PlayerAlertState());
    }

    public void publish(Alert alert) {
        Objects.requireNonNull(alert, "alert");
        PlayerAlertState state = getState(alert.getPlayerId());
        state.incrementAndGet();
        state.setLastAlertMillis(alert.getTimestampMillis());
        for (AlertSink sink : sinks) {
            sink.publish(alert);
        }
    }
}
