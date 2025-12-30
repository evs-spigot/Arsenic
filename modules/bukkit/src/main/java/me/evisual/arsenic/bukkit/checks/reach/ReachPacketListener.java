package me.evisual.arsenic.bukkit.checks.reach;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class ReachPacketListener extends PacketAdapter {
    private final JavaPlugin plugin;
    private final ReachService reachService;
    private final EntityPositionTracker tracker;

    public ReachPacketListener(JavaPlugin plugin, ReachService reachService, EntityPositionTracker tracker) {
        super(plugin, PacketType.Play.Client.USE_ENTITY);
        this.plugin = plugin;
        this.reachService = reachService;
        this.tracker = tracker;
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        Player attacker = event.getPlayer();
        boolean debug = plugin.getConfig().getBoolean("checks.reach.debug");
        EnumWrappers.EntityUseAction action = readAction(packet);
        if (action != EnumWrappers.EntityUseAction.ATTACK) {
            if (debug) {
                plugin.getLogger().info("[Reach] Ignored USE_ENTITY action=" + action);
            }
            return;
        }
        int entityId;
        try {
            entityId = packet.getIntegers().read(0);
        } catch (Exception ex) {
            if (debug) {
                plugin.getLogger().info("[Reach] Failed to read entity id");
            }
            return;
        }

        long now = System.currentTimeMillis();
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player liveAttacker = Bukkit.getPlayer(attacker.getUniqueId());
            if (liveAttacker == null) {
                return;
            }
            Entity target = findByEntityId(liveAttacker, entityId);
            if (target == null) {
                if (debug) {
                    plugin.getLogger().info("[Reach] Target entity not found for id=" + entityId);
                }
                return;
            }
            int ping = PingUtil.getPing(liveAttacker);
            long targetTime = now - (ping / 2L) - 50L;
            EntityPositionTracker.Sample sample = tracker.getClosestSample(target.getUniqueId(), targetTime);
            if (debug && sample == null) {
                plugin.getLogger().info("[Reach] No sample for target " + target.getUniqueId());
            }
            reachService.handleAttack(liveAttacker, target, sample, now);
        });
    }

    private EnumWrappers.EntityUseAction readAction(PacketContainer packet) {
        try {
            return packet.getEntityUseActions().read(0);
        } catch (Exception ignored) {
            try {
                com.comphenix.protocol.wrappers.WrappedEnumEntityUseAction wrapped =
                        packet.getEnumEntityUseActions().read(0);
                if (wrapped == null) {
                    return null;
                }
                return wrapped.getAction();
            } catch (Exception ignored2) {
                return null;
            }
        }
    }

    private Entity findByEntityId(Player attacker, int entityId) {
        for (Entity entity : attacker.getWorld().getEntities()) {
            if (entity.getEntityId() == entityId) {
                return entity;
            }
        }
        return null;
    }
}
