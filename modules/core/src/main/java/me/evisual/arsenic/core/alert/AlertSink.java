package me.evisual.arsenic.core.alert;

public interface AlertSink {
    void publish(Alert alert);
}
