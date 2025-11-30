package dev.oumaimaa.data;

import dev.oumaimaa.KawaiiAdPlugin;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Manages player cooldowns for advertisement broadcasts.
 * Cooldowns are persisted via DatabaseManager. Optimized to minimize autoboxing.
 */
public final class CooldownManager {

    private static final String COOLDOWN_BYPASS_PERMISSION = "kawaiid.bypass";
    private static final long SECONDS_IN_MINUTE = 60L;
    private static final long SECONDS_IN_HOUR = 3600L;
    private static final long SECONDS_IN_DAY = 86400L;
    private final KawaiiAdPlugin plugin;
    private final DatabaseManager databaseManager;
    private final Map<UUID, Long> cooldownData = new ConcurrentHashMap<>();

    /**
     * Constructs the CooldownManager.
     *
     * @param plugin The main plugin instance.
     */
    public CooldownManager(final @NotNull KawaiiAdPlugin plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    /**
     * Loads a player's cooldown from the database into memory upon command execution.
     *
     * @param player The player.
     * @return The last ad timestamp in milliseconds, or 0L if none found.
     */
    private long loadCooldownFromDatabase(final @NotNull Player player) {
        final Long cachedTime = cooldownData.get(player.getUniqueId());
        if (cachedTime != null) {
            return cachedTime.longValue();
        }

        final long timestamp = databaseManager.loadCooldown(player.getUniqueId()).orElse(0L);
        if (timestamp > 0) {
            cooldownData.put(player.getUniqueId(), timestamp);
        }
        return timestamp;
    }

    /**
     * Finds the shortest cooldown duration in seconds applicable to the player based on their permissions.
     *
     * @param player The player to check.
     * @return The lowest cooldown time in seconds.
     */
    public long getEffectiveCooldownSeconds(final Player player) {
        final long lowestCooldown = plugin.getAdsConfigManager().getDefaultCooldown();

        return plugin.getAdsConfigManager().getRankCooldowns().entrySet().stream()
                .filter(entry -> player.hasPermission(entry.getKey()))
                .mapToLong(Map.Entry::getValue)
                .min()
                .orElse(lowestCooldown);
    }

    /**
     * Checks if a player is currently on cooldown. Loads the data from the DB if necessary.
     *
     * @param player The player to check.
     * @return The remaining time in seconds, or 0 if no cooldown applies.
     */
    public long getRemainingCooldown(final @NotNull Player player) {
        if (player.hasPermission(COOLDOWN_BYPASS_PERMISSION)) {
            return 0;
        }

        final long lastAdTime = loadCooldownFromDatabase(player);

        if (lastAdTime == 0L) {
            return 0;
        }

        final long requiredCooldownMillis = TimeUnit.SECONDS.toMillis(getEffectiveCooldownSeconds(player));
        final long currentTime = System.currentTimeMillis();
        final long cooldownEndTime = lastAdTime + requiredCooldownMillis;

        if (currentTime < cooldownEndTime) {
            return (cooldownEndTime - currentTime) / 1000;
        }
        return 0;
    }

    /**
     * Applies the cooldown to the player, storing it in memory and scheduling a save to the database.
     *
     * @param player The player to apply the cooldown to.
     */
    public void applyCooldown(final @NotNull Player player) {
        final long currentTime = System.currentTimeMillis();
        final UUID uuid = player.getUniqueId();

        cooldownData.put(uuid, currentTime);

        databaseManager.saveCooldown(uuid, currentTime);
    }

    /**
     * Saves all currently tracked cooldowns to the database (used on plugin shutdown).
     */
    public void saveAllCooldownsAsync() {
        cooldownData.forEach(databaseManager::saveCooldown);
    }

    /**
     * Formats remaining seconds into a readable string (e.g., "5m 30s").
     *
     * @param seconds The total number of seconds remaining.
     * @return A formatted time string.
     */
    public @NotNull String formatTime(long seconds) {
        if (seconds <= 0) return "0s";

        final long days = seconds / SECONDS_IN_DAY;
        final long hours = (seconds % SECONDS_IN_DAY) / SECONDS_IN_HOUR;
        final long minutes = (seconds % SECONDS_IN_HOUR) / SECONDS_IN_MINUTE;
        final long remainingSeconds = seconds % SECONDS_IN_MINUTE;

        final StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (remainingSeconds > 0 || sb.isEmpty()) sb.append(remainingSeconds).append("s");

        return sb.toString().trim();
    }
}