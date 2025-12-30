package me.evisual.arsenic.bukkit.banwaves;

import me.evisual.arsenic.core.alert.Alert;
import me.evisual.arsenic.core.alert.AlertSink;

public final class BanWaveAlertSink implements AlertSink {
    private final BanWaveService service;

    public BanWaveAlertSink(BanWaveService service) {
        this.service = service;
    }

    @Override
    public void publish(Alert alert) {
        service.consider(alert.getPlayerId(), alert.getPlayerName());
    }
}
