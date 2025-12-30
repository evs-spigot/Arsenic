package me.evisual.arsenic.bukkit.session;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class SessionListener implements Listener {
    private final SessionService sessionService;

    public SessionListener(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        sessionService.startSession(event.getPlayer(), System.currentTimeMillis());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        sessionService.endSession(event.getPlayer());
    }
}
