package me.evisual.arsenic.bukkit;

import me.evisual.arsenic.core.alert.AlertService;
import org.bukkit.plugin.java.JavaPlugin;

public final class ArsenicPlugin extends JavaPlugin {
    private ConfigService configService;
    private MessageService messageService;
    private AlertService alertService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        configService = new ConfigService(this);
        messageService = new MessageService(this);
        alertService = new AlertService();

        alertService.registerSink(new AlertNotifier(this, configService, messageService));
        if (getCommand("arsenic") != null) {
            getCommand("arsenic").setExecutor(new me.evisual.arsenic.bukkit.command.ArsenicCommand(this));
        }
        getLogger().info("Arsenic enabled.");
    }

    @Override
    public void onDisable() {
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
}
