package dev.oumaimaa.papi;

import dev.oumaimaa.KawaiiAdPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Custom PlaceholderAPI expansion for KawaiiAD, providing debug and cooldown status.
 */
public final class Placeholder extends PlaceholderExpansion {

    private final KawaiiAdPlugin plugin;
    private final String version;

    /**
     * Constructs the PAPI expansion.
     *
     * @param plugin  The main plugin instance.
     * @param version The plugin version.
     */
    public Placeholder(final KawaiiAdPlugin plugin, final String version) {
        this.plugin = plugin;
        this.version = version;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "kawaiiads";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getPluginMeta().getAuthors().getFirst();
    }

    @Override
    public @NotNull String getVersion() {
        return version;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(final OfflinePlayer player, @NotNull final String identifier) {
        if (player == null || !player.isOnline()) return null;

        if (identifier.equals("cooldown_remaining")) {
            final long remaining = plugin.getCooldownManager().getRemainingCooldown(Objects.requireNonNull(player.getPlayer()));
            return plugin.getCooldownManager().formatTime(remaining);
        }

        if (identifier.equals("is_on_cooldown")) {
            final long remaining = plugin.getCooldownManager().getRemainingCooldown(Objects.requireNonNull(player.getPlayer()));
            return remaining > 0 ? "true" : "false";
        }

        return null;
    }
}