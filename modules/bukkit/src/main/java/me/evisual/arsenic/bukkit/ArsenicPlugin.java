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
    private me.evisual.arsenic.bukkit.checks.autoclicker.AutoClickerService autoClickerService;
    private me.evisual.arsenic.bukkit.checks.reach.ReachService reachService;
    private me.evisual.arsenic.bukkit.checks.reach.EntityPositionTracker reachPositionTracker;
    private me.evisual.arsenic.bukkit.session.SessionService sessionService;
    private me.evisual.arsenic.bukkit.trust.TrustScoreService trustScoreService;
    private me.evisual.arsenic.bukkit.banwaves.BanWaveService banWaveService;

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
            banWaveService = new me.evisual.arsenic.bukkit.banwaves.BanWaveService(this, reportStore);
            alertService.registerSink(new me.evisual.arsenic.bukkit.banwaves.BanWaveAlertSink(banWaveService));
        } catch (Exception ex) {
            getLogger().severe("Failed to initialize report database: " + ex.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (getCommand("arsenic") != null) {
            trustScoreService = new me.evisual.arsenic.bukkit.trust.TrustScoreService(this, reportStore);
            reportGuiService = new me.evisual.arsenic.bukkit.gui.ReportGuiService(this, reportStore, trustScoreService);
            sessionService = new me.evisual.arsenic.bukkit.session.SessionService();
            getServer().getPluginManager().registerEvents(new me.evisual.arsenic.bukkit.session.SessionListener(sessionService), this);
            me.evisual.arsenic.bukkit.command.ArsenicCommand command = new me.evisual.arsenic.bukkit.command.ArsenicCommand(this, reportStore, reportGuiService, sessionService, trustScoreService);
            getCommand("arsenic").setExecutor(command);
            getCommand("arsenic").setTabCompleter(command);
            getServer().getPluginManager().registerEvents(new me.evisual.arsenic.bukkit.gui.ReportGuiListener(reportGuiService), this);
        }

        autoClickerService = new me.evisual.arsenic.bukkit.checks.autoclicker.AutoClickerService(this, configService, alertService);
        if (getServer().getPluginManager().getPlugin("ProtocolLib") != null) {
            com.comphenix.protocol.ProtocolLibrary.getProtocolManager()
                    .addPacketListener(new me.evisual.arsenic.bukkit.checks.autoclicker.AutoClickerPacketListener(this, autoClickerService));
        } else {
            getLogger().warning("ProtocolLib not found; autoclicker detection is using fallback events.");
            getServer().getPluginManager().registerEvents(new me.evisual.arsenic.bukkit.checks.autoclicker.AutoClickerListener(autoClickerService), this);
        }

        reachService = new me.evisual.arsenic.bukkit.checks.reach.ReachService(this, configService, alertService);
        reachPositionTracker = new me.evisual.arsenic.bukkit.checks.reach.EntityPositionTracker(this);
        reachPositionTracker.start();
        if (getServer().getPluginManager().getPlugin("ProtocolLib") != null) {
            com.comphenix.protocol.ProtocolLibrary.getProtocolManager()
                    .addPacketListener(new me.evisual.arsenic.bukkit.checks.reach.ReachPacketListener(this, reachService, reachPositionTracker));
        } else {
            getLogger().warning("ProtocolLib not found; reach detection is disabled.");
        }

        if (banWaveService != null) {
            banWaveService.start();
        }
        getLogger().info("Arsenic enabled.");
    }

    @Override
    public void onDisable() {
        if (reachPositionTracker != null) {
            reachPositionTracker.stop();
        }
        if (banWaveService != null) {
            banWaveService.stop();
        }
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

    public me.evisual.arsenic.bukkit.checks.autoclicker.AutoClickerService getAutoClickerService() {
        return autoClickerService;
    }

    public me.evisual.arsenic.bukkit.session.SessionService getSessionService() {
        return sessionService;
    }

    public me.evisual.arsenic.bukkit.trust.TrustScoreService getTrustScoreService() {
        return trustScoreService;
    }

    public me.evisual.arsenic.bukkit.banwaves.BanWaveService getBanWaveService() {
        return banWaveService;
    }
}
