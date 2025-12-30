package me.evisual.arsenic.bukkit.checks.autoclicker;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AutoClickerPacketListener extends PacketAdapter {
    private static final long ABORT_GRACE_MS = 150L;
    private static final long ARM_CORRELATION_MS = 100L;

    private final AutoClickerService service;
    private final Map<UUID, Boolean> digging = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastAbort = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastArm = new ConcurrentHashMap<>();

    public AutoClickerPacketListener(JavaPlugin plugin, AutoClickerService service) {
        super(plugin, PacketType.Play.Client.ARM_ANIMATION, PacketType.Play.Client.BLOCK_DIG);
        this.service = service;
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        PacketContainer packet = event.getPacket();
        long now = System.currentTimeMillis();

        if (packet.getType() == PacketType.Play.Client.BLOCK_DIG) {
            EnumWrappers.PlayerDigType digType = packet.getPlayerDigTypes().read(0);
            if (digType == EnumWrappers.PlayerDigType.START_DESTROY_BLOCK) {
                if (canStartDig(player, now) && isArmCorrelated(player, now)) {
                    digging.put(player.getUniqueId(), true);
                    service.handleClick(player, now, false);
                }
            } else if (digType == EnumWrappers.PlayerDigType.ABORT_DESTROY_BLOCK
                    || digType == EnumWrappers.PlayerDigType.STOP_DESTROY_BLOCK) {
                digging.put(player.getUniqueId(), false);
                lastAbort.put(player.getUniqueId(), now);
            }
            return;
        }
        if (packet.getType() == PacketType.Play.Client.ARM_ANIMATION) {
            lastArm.put(player.getUniqueId(), now);
            boolean ignore = isDigging(player);
            service.handleClick(player, now, ignore);
        }
    }

    private boolean isDigging(Player player) {
        Boolean active = digging.get(player.getUniqueId());
        return active != null && active;
    }

    private boolean canStartDig(Player player, long now) {
        if (isDigging(player)) {
            return false;
        }
        Long last = lastAbort.get(player.getUniqueId());
        return last == null || now - last >= ABORT_GRACE_MS;
    }

    private boolean isArmCorrelated(Player player, long now) {
        Long last = lastArm.get(player.getUniqueId());
        return last != null && now - last <= ARM_CORRELATION_MS;
    }
}
