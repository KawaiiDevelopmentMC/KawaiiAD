package dev.oumaimaa;

import dev.oumaimaa.commands.AdsCommand;
import dev.oumaimaa.commands.AdsTabCompleter;
import dev.oumaimaa.commands.HelpCommand;
import dev.oumaimaa.config.AdsConfigManager;
import dev.oumaimaa.data.CooldownManager;
import dev.oumaimaa.data.DatabaseManager;
import dev.oumaimaa.papi.Placeholder;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main class for the KawaiiAD Plugin.
 * Handles plugin lifecycle, configuration loading, and stores ephemeral data like pending ads.
 */
public final class KawaiiAdPlugin extends JavaPlugin {

    private final Map<UUID, String> pendingAds = new ConcurrentHashMap<>();

    private AdsConfigManager configManager;
    private DatabaseManager databaseManager;
    private CooldownManager cooldownManager;

    @Override
    public void onEnable() {
        this.configManager = new AdsConfigManager(this);
        this.configManager.setupConfig();
        this.databaseManager = new DatabaseManager(this);
        this.cooldownManager = new CooldownManager(this);
        Objects.requireNonNull(this.getCommand("ads")).setExecutor(new AdsCommand(this));
        Objects.requireNonNull(this.getCommand("ads")).setTabCompleter(new AdsTabCompleter(this));
        Objects.requireNonNull(this.getCommand("kawaiiadshelp")).setExecutor(new HelpCommand());

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new Placeholder(this, getPluginMeta().getVersion()).register();
            getLogger().info("PlaceholderAPI expansion registered.");
        }

        getLogger().info("KawaiiAD enabled.");
    }

    @Override
    public void onDisable() {
        if (cooldownManager != null) {
            cooldownManager.saveAllCooldownsAsync();
        }
        if (databaseManager != null) {
            databaseManager.closePool();
        }
        getLogger().info("KawaiiAD disabled.");
    }

    /**
     * Retrieves the database manager instance.
     *
     * @return The DatabaseManager instance.
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    /**
     * Retrieves the map of players with pending ad confirmations.
     *
     * @return The map of pending ads.
     */
    public Map<UUID, String> getPendingAds() {
        return pendingAds;
    }

    /**
     * Retrieves the configuration manager instance.
     *
     * @return The AdsConfigManager instance.
     */
    public AdsConfigManager getAdsConfigManager() {
        return configManager;
    }

    /**
     * Retrieves the cooldown manager instance.
     *
     * @return The CooldownManager instance.
     */
    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }
}