package me.evisual.arsenic.bukkit.checks.autoclicker;

import me.evisual.arsenic.bukkit.ConfigService;
import me.evisual.arsenic.core.alert.Alert;
import me.evisual.arsenic.core.alert.AlertService;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class AutoClickerServiceTest {

    @Test
    void cpsCheckFires() {
        YamlConfiguration config = baseConfig();
        config.set("checks.autoclicker.cps.enabled", true);
        config.set("checks.autoclicker.cps.threshold", 10);
        config.set("checks.autoclicker.consistency.enabled", false);
        config.set("checks.autoclicker.pattern.enabled", false);
        config.set("checks.autoclicker.tick-align.enabled", false);

        TestHarness harness = new TestHarness(config);
        long now = System.currentTimeMillis();
        for (int i = 0; i < 12; i++) {
            harness.service.handleClick(harness.player, now + i * 70L, false, false);
        }

        assertEquals(1, harness.alerts.size());
        assertEquals("Autoclicker CPS", harness.alerts.get(0).getCheckName());
    }

    @Test
    void consistencyCheckFires() {
        YamlConfiguration config = baseConfig();
        config.set("checks.autoclicker.cps.enabled", false);
        config.set("checks.autoclicker.consistency.enabled", true);
        config.set("checks.autoclicker.consistency.min-samples", 12);
        config.set("checks.autoclicker.consistency.max-stddev-ms", 1.0);
        config.set("checks.autoclicker.consistency.min-cps", 8.0);
        config.set("checks.autoclicker.pattern.enabled", false);
        config.set("checks.autoclicker.tick-align.enabled", false);

        TestHarness harness = new TestHarness(config);
        long now = System.currentTimeMillis();
        for (int i = 0; i < 13; i++) {
            harness.service.handleClick(harness.player, now + i * 100L, false, false);
        }

        assertEquals(1, harness.alerts.size());
        assertEquals("Autoclicker Consistency", harness.alerts.get(0).getCheckName());
    }

    @Test
    void patternCheckFires() {
        YamlConfiguration config = baseConfig();
        config.set("checks.autoclicker.cps.enabled", false);
        config.set("checks.autoclicker.consistency.enabled", false);
        config.set("checks.autoclicker.pattern.enabled", true);
        config.set("checks.autoclicker.pattern.min-samples", 10);
        config.set("checks.autoclicker.pattern.max-mode-ratio", 0.6);
        config.set("checks.autoclicker.pattern.min-cps", 6.0);
        config.set("checks.autoclicker.tick-align.enabled", false);

        TestHarness harness = new TestHarness(config);
        long now = System.currentTimeMillis();
        for (int i = 0; i < 11; i++) {
            harness.service.handleClick(harness.player, now + i * 120L, false, false);
        }

        assertEquals(1, harness.alerts.size());
        assertEquals("Autoclicker Pattern", harness.alerts.get(0).getCheckName());
    }

    @Test
    void tickAlignIgnoresBlockClicks() {
        YamlConfiguration config = baseConfig();
        config.set("checks.autoclicker.cps.enabled", false);
        config.set("checks.autoclicker.consistency.enabled", false);
        config.set("checks.autoclicker.pattern.enabled", false);
        config.set("checks.autoclicker.tick-align.enabled", true);
        config.set("checks.autoclicker.tick-align.min-samples", 6);
        config.set("checks.autoclicker.tick-align.min-aligned-ratio", 0.9);
        config.set("checks.autoclicker.tick-align.min-cps", 6.0);
        config.set("checks.autoclicker.tick-align.tolerance-ms", 2);

        TestHarness harness = new TestHarness(config);
        long base = 1_000_000L;
        for (int i = 0; i < 7; i++) {
            harness.service.handleClick(harness.player, base + (i * 50L), false, true);
        }

        assertEquals(0, harness.alerts.size());
    }

    @Test
    void tickAlignFiresOnNonBlockClicks() {
        YamlConfiguration config = baseConfig();
        config.set("checks.autoclicker.cps.enabled", false);
        config.set("checks.autoclicker.consistency.enabled", false);
        config.set("checks.autoclicker.pattern.enabled", false);
        config.set("checks.autoclicker.tick-align.enabled", true);
        config.set("checks.autoclicker.tick-align.min-samples", 6);
        config.set("checks.autoclicker.tick-align.min-aligned-ratio", 0.9);
        config.set("checks.autoclicker.tick-align.min-cps", 6.0);
        config.set("checks.autoclicker.tick-align.tolerance-ms", 2);

        TestHarness harness = new TestHarness(config);
        long base = 2_000_000L;
        for (int i = 0; i < 7; i++) {
            harness.service.handleClick(harness.player, base + (i * 50L), false, false);
        }

        assertEquals(1, harness.alerts.size());
        assertEquals("Autoclicker TickAlign", harness.alerts.get(0).getCheckName());
    }

    private YamlConfiguration baseConfig() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("checks.autoclicker.enabled", true);
        config.set("checks.autoclicker.cps.enabled", false);
        config.set("checks.autoclicker.consistency.enabled", false);
        config.set("checks.autoclicker.pattern.enabled", false);
        config.set("checks.autoclicker.tick-align.enabled", false);
        return config;
    }

    private static final class TestHarness {
        private final AutoClickerService service;
        private final Player player;
        private final List<Alert> alerts = new ArrayList<>();

        private TestHarness(YamlConfiguration config) {
            ConfigService configService = mockConfigService(config);
            AlertService alertService = new AlertService();
            alertService.registerSink(alerts::add);
            JavaPlugin plugin = Mockito.mock(JavaPlugin.class);
            player = mockPlayer();
            service = new AutoClickerService(plugin, configService, alertService);
        }

        private static Player mockPlayer() {
            Player player = Mockito.mock(Player.class);
            Mockito.when(player.getUniqueId()).thenReturn(UUID.randomUUID());
            Mockito.when(player.getName()).thenReturn("TestPlayer");
            Mockito.when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
            return player;
        }

        private static ConfigService mockConfigService(YamlConfiguration config) {
            ConfigService service = Mockito.mock(ConfigService.class);
            Mockito.when(service.getConfig()).thenReturn(config);
            Mockito.when(service.getBoolean(Mockito.anyString()))
                    .thenAnswer(inv -> config.getBoolean(inv.getArgument(0)));
            Mockito.when(service.getString(Mockito.anyString(), Mockito.anyString()))
                    .thenAnswer(inv -> config.getString(inv.getArgument(0), inv.getArgument(1)));
            return service;
        }
    }
}
