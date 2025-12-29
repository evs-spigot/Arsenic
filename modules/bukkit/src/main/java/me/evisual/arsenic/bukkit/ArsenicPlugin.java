package me.evisual.arsenic.bukkit;

import me.evisual.arsenic.core.alert.AlertService;
import me.evisual.arsenic.core.storage.ReportStore;
import org.bukkit.plugin.java.JavaPlugin;

public final class ArsenicPlugin extends JavaPlugin {
    private ConfigService configService;
    private MessageService messageService;
    private AlertService alertService;
    private ReportStore reportStore;
    private me.evisual.arsenic.bukkit.gui.ReportGuiService reportGuiService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        configService = new ConfigService(this);
        messageService = new MessageService(this);
        alertService = new AlertService();

        alertService.registerSink(new AlertNotifier(this, configService, messageService));
        reportStore = me.evisual.arsenic.bukkit.storage.ReportStoreFactory.create(this, configService);
        try {
            reportStore.init();
            alertService.registerSink(new me.evisual.arsenic.bukkit.storage.AlertPersistenceSink(reportStore, getLogger()));
        } catch (Exception ex) {
            getLogger().severe("Failed to initialize report database: " + ex.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (getCommand("arsenic") != null) {
            reportGuiService = new me.evisual.arsenic.bukkit.gui.ReportGuiService(this, reportStore);
            me.evisual.arsenic.bukkit.command.ArsenicCommand command = new me.evisual.arsenic.bukkit.command.ArsenicCommand(this, reportStore, reportGuiService);
            getCommand("arsenic").setExecutor(command);
            getCommand("arsenic").setTabCompleter(command);
            getServer().getPluginManager().registerEvents(new me.evisual.arsenic.bukkit.gui.ReportGuiListener(reportGuiService), this);
        }
        getLogger().info("Arsenic enabled.");
    }

    @Override
    public void onDisable() {
        if (reportStore != null) {
            try {
                reportStore.close();
            } catch (Exception ex) {
                getLogger().warning("Failed to close report database: " + ex.getMessage());
            }
        }
        getLogger().info("Arsenic disabled.");
    }

    public ConfigService getConfigService() {
        return configService;
    }

    public MessageService getMessageService() {
        return messageService;
    }

    public AlertService getAlertService() {
        return alertService;
    }

    public ReportStore getReportStore() {
        return reportStore;
    }

    public me.evisual.arsenic.bukkit.gui.ReportGuiService getReportGuiService() {
        return reportGuiService;
    }
}
