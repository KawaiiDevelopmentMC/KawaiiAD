package dev.oumaimaa.config;

import dev.oumaimaa.KawaiiAdPlugin;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Source;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages plugin configuration, providing type-safe access to settings
 * and handling color code translation for messages. Optimizes performance by caching static messages.
 */
public final class AdsConfigManager {

    private final KawaiiAdPlugin plugin;
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.builder().character('&').hexColors().build();
    private FileConfiguration config;
    private Map<String, Long> rankCooldowns = Collections.emptyMap();

    private Component cachedNoPermission;
    private Component cachedCooldownBypass;
    private Component cachedPreviewHeader;
    private Component cachedPreviewFooter;
    private Component cachedAdBroadcasted;
    private Component cachedAdCancelled;
    private Component cachedAdTimeout;
    private Component cachedAdAlreadyPending;
    private Component cachedUsage;
    private Component cachedAdPrefix;
    private Component cachedAdQueuedStaff; // New cached component
    private Component cachedAdQueuedPlayer; // New cached component

    /**
     * Constructs the Configuration Manager.
     *
     * @param plugin The main plugin instance.
     */
    public AdsConfigManager(final KawaiiAdPlugin plugin) {
        this.plugin = plugin;
    }

    public void setupConfig() {
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
        loadCooldowns();
        loadCachedMessages();
    }

    private void loadCooldowns() {
        final Map<String, Long> tempCooldowns = new HashMap<>();
        final ConfigurationSection section = config.getConfigurationSection("cooldowns.ranks");

        if (section != null) {
            for (final String key : section.getKeys(false)) {
                final String permission = "kawaiid.cooldown." + key.toLowerCase();
                final long timeInSeconds = config.getLong("cooldowns.ranks." + key, getDefaultCooldown());
                tempCooldowns.put(permission, timeInSeconds);
            }
        }
        this.rankCooldowns = Collections.unmodifiableMap(tempCooldowns);
    }

    /**
     * Loads and caches static messages into Component objects for performance.
     */
    private void loadCachedMessages() {
        this.cachedNoPermission = loadStaticComponent("messages.no-permission");
        this.cachedCooldownBypass = loadStaticComponent("messages.cooldown-bypass");
        this.cachedPreviewHeader = loadStaticComponent("messages.preview-header");
        this.cachedPreviewFooter = loadStaticComponent("messages.preview-footer");
        this.cachedAdBroadcasted = loadStaticComponent("messages.ad-broadcasted");
        this.cachedAdCancelled = loadStaticComponent("messages.ad-cancelled");
        this.cachedAdTimeout = loadStaticComponent("messages.ad-timeout");
        this.cachedAdAlreadyPending = loadStaticComponent("messages.ad-already-pending");
        this.cachedUsage = loadStaticComponent("messages.usage");
        this.cachedAdPrefix = loadStaticComponent("messages.preview-ad-prefix");
        this.cachedAdQueuedStaff = loadStaticComponent("messages.ad-queued-staff-alert");
        this.cachedAdQueuedPlayer = loadStaticComponent("messages.ad-queued-player-confirm");
    }

    private @NotNull Component loadStaticComponent(final String path) {
        final String message = config.getString(path, "Message not found: " + path);
        return serializer.deserialize(message);
    }

    public Component getMessage(final String path, final String @NotNull ... placeholders) {
        final String message = config.getString("messages." + path, "Message not found: " + path);
        Component component = serializer.deserialize(message);

        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                final String key = placeholders[i];
                final String value = placeholders[i + 1];
                component = Component.text(serializer.serialize(component).replace(key, value));
            }
        }

        return component;
    }

    /**
     * @return true if manual review is required for all submitted ads.
     */
    public boolean isReviewRequired() {
        return config.getBoolean("moderation.require-review", false);
    }

    public boolean isDebugEnabled() {
        return config.getBoolean("settings.debug-mode", false);
    }

    public @NotNull List<String> getProfanityFilter() {
        return config.getStringList("moderation.profanity-filter");
    }

    public int getMinLength() {
        return config.getInt("moderation.min-length", 10);
    }

    public int getMaxLength() {
        return config.getInt("moderation.max-length", 150);
    }

    public @NotNull Sound getBroadcastSound() {
        final String soundKey = config.getString("broadcast-sound.key", "minecraft:entity.experience_orb.pickup");
        final double volume = config.getDouble("broadcast-sound.volume", 1.0);
        final double pitch = config.getDouble("broadcast-sound.pitch", 1.5);

        return Sound.sound(Key.key(soundKey), Source.MASTER, (float) volume, (float) pitch);
    }

    public long getDefaultCooldown() {
        return config.getLong("cooldowns.default", 300L);
    }

    public long getConfirmationTimeoutSeconds() {
        return config.getLong("confirmation-timeout-seconds", 60L);
    }

    public Map<String, Long> getRankCooldowns() {
        return rankCooldowns;
    }

    public Component getCachedNoPermission() {
        return cachedNoPermission;
    }

    public Component getCachedCooldownBypass() {
        return cachedCooldownBypass;
    }

    public Component getCachedPreviewHeader() {
        return cachedPreviewHeader;
    }

    public Component getCachedPreviewFooter() {
        return cachedPreviewFooter;
    }

    public Component getCachedAdBroadcasted() {
        return cachedAdBroadcasted;
    }

    public Component getCachedAdCancelled() {
        return cachedAdCancelled;
    }

    public Component getCachedAdTimeout() {
        return cachedAdTimeout;
    }

    public Component getCachedAdAlreadyPending() {
        return cachedAdAlreadyPending;
    }

    public Component getCachedUsage() {
        return cachedUsage;
    }

    public Component getCachedAdPrefix() {
        return cachedAdPrefix;
    }

    public Component getCachedAdQueuedStaff() {
        return cachedAdQueuedStaff;
    }

    public Component getCachedAdQueuedPlayer() {
        return cachedAdQueuedPlayer;
    }

    public FileConfiguration getConfig() {
        return config;
    }
}