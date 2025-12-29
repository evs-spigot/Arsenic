package me.evisual.arsenic.bukkit.storage;

import me.evisual.arsenic.bukkit.ConfigService;
import me.evisual.arsenic.core.storage.ReportStore;
import org.bukkit.plugin.java.JavaPlugin;

public final class ReportStoreFactory {
    private ReportStoreFactory() {
    }

    public static ReportStore create(JavaPlugin plugin, ConfigService configService) {
        String typeValue = configService.getString("database.type");
        DatabaseType type;
        try {
            type = DatabaseType.valueOf(typeValue.trim().toUpperCase());
        } catch (Exception ex) {
            type = DatabaseType.SQLITE;
        }

        if (type == DatabaseType.SQLITE) {
            return new SqliteReportStore(plugin, configService);
        }

        return new SqliteReportStore(plugin, configService);
    }
}
