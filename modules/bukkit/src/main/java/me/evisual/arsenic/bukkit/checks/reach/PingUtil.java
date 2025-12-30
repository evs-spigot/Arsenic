package me.evisual.arsenic.bukkit.checks.reach;

import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class PingUtil {
    private PingUtil() {
    }

    public static int getPing(Player player) {
        try {
            Method getHandle = player.getClass().getMethod("getHandle");
            Object handle = getHandle.invoke(player);
            Field pingField = handle.getClass().getField("ping");
            return pingField.getInt(handle);
        } catch (Exception ignored) {
            return 0;
        }
    }
}
