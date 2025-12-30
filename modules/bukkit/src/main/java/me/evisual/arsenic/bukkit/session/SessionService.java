package me.evisual.arsenic.bukkit.session;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SessionService {
    private final Map<UUID, Long> sessionStart = new ConcurrentHashMap<>();

    public void startSession(Player player, long timestamp) {
        sessionStart.put(player.getUniqueId(), timestamp);
    }

    public void endSession(Player player) {
        sessionStart.remove(player.getUniqueId());
    }

    public Long getSessionStart(UUID playerId) {
        return sessionStart.get(playerId);
    }
}
