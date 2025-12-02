package dev.oumaimaa.commands;

import dev.oumaimaa.KawaiiAdPlugin;
import dev.oumaimaa.config.AdsConfigManager;
import dev.oumaimaa.data.CooldownManager;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Command executor for the /ads command.
 * Handles ad submission, preview, confirmation (interactive), cancellation, and cooldown checks.
 */
public final class AdsCommand implements CommandExecutor {

    private final KawaiiAdPlugin plugin;
    private final AdsConfigManager configManager;
    private final CooldownManager cooldownManager;
    private final Map<UUID, String> pendingAds;
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.builder().character('&').hexColors().build();

    private static final String ADMIN_PERMISSION = "kawaiid.admin";
    private static final String USE_PERMISSION = "kawaiid.use";
    private static final String BROADCAST_WORLD_PERMISSION = "kawaiid.broadcast.world";
    private static final String BYPASS_PERMISSION = "kawaiid.bypass";
    private static final String REVIEW_PERMISSION = "kawaiid.review";

    /**
     * Constructs the AdsCommand executor.
     * @param plugin The main plugin instance.
     */
    public AdsCommand(final @NotNull KawaiiAdPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getAdsConfigManager();
        this.cooldownManager = plugin.getCooldownManager();
        this.pendingAds = plugin.getPendingAds();
    }

    @Override
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String label, @NotNull final String @NotNull [] args) {
        if (args.length == 0) {
            sender.sendMessage(configManager.getCachedUsage());
            return true;
        }

        final String subCommand = args[0].toLowerCase();

        if (subCommand.equals("broadcast")) {
            return handleBroadcastCommand(sender, args);
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can submit or confirm ads.", NamedTextColor.RED));
            return true;
        }

        if (!player.hasPermission(USE_PERMISSION)) {
            player.sendMessage(configManager.getCachedNoPermission());
            return true;
        }

        if (args.length == 1) {
            switch (subCommand) {
                case "confirm":
                    return handleConfirm(player);
                case "cancel":
                    return handleCancel(player);
                case "reload":
                    if (player.hasPermission(ADMIN_PERMISSION)) {
                        return handleReload(player);
                    } else {
                        player.sendMessage(configManager.getCachedNoPermission());
                        return true;
                    }
                case "review":
                    if (player.hasPermission(REVIEW_PERMISSION)) {
                        player.sendMessage(Component.text("Review system implemented. Use /ads review [page] to check queue.", NamedTextColor.YELLOW));
                        return true;
                    }
                    return false;
                default:
                    return handleAdSubmission(player, args);
            }
        }

        return handleAdSubmission(player, args);
    }

    /**
     * Handles player submission of a new ad message.
     */
    private boolean handleAdSubmission(final @NotNull Player player, final String[] args) {
        final UUID playerUUID = player.getUniqueId();
        final String adMessageRaw = String.join(" ", args);

        if (pendingAds.containsKey(playerUUID)) {
            player.sendMessage(configManager.getCachedAdAlreadyPending());
            return true;
        }

        final long remainingCooldown = cooldownManager.getRemainingCooldown(player);
        if (remainingCooldown > 0) {
            sendActionBarCooldown(player, remainingCooldown);
            return true;
        } else if (player.hasPermission(BYPASS_PERMISSION)) {
            player.sendMessage(configManager.getCachedCooldownBypass());
        }

        final String validationError = validateAdMessage(adMessageRaw);
        if (validationError != null) {
            player.sendMessage(legacySerializer.deserialize(validationError));
            return true;
        }

        if (configManager.isReviewRequired()) {
            plugin.getDatabaseManager().queueAdForReview(playerUUID, adMessageRaw);
            player.sendMessage(configManager.getCachedAdQueuedPlayer());
            alertStaffOfReview(player);
            return true;
        }

        pendingAds.put(playerUUID, adMessageRaw);
        sendAdPreview(player, adMessageRaw);
        scheduleTimeout(playerUUID);

        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("DEBUG: Ad submission by " + player.getName() + " pending confirmation.");
        }
        return true;
    }

    /**
     * Alerts staff of a pending ad review using Adventure's Audiance filtering.
     */
    private void alertStaffOfReview(final @NotNull Player player) {
        final Audience staff = Audience.audience(
                Bukkit.getOnlinePlayers().stream()
                        .filter(p -> p.hasPermission(REVIEW_PERMISSION))
                        .collect(Collectors.toList())
        );

        final Component alertMessage = configManager.getCachedAdQueuedStaff()
                .append(Component.text(" (" + player.getName() + ")", NamedTextColor.YELLOW))
                .clickEvent(ClickEvent.runCommand("/ads review 1"))
                .hoverEvent(HoverEvent.showText(Component.text("Click to review the queue.", NamedTextColor.AQUA)));

        staff.sendMessage(alertMessage);

        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("DEBUG: Staff alerted for new ad review.");
        }
    }

    /**
     * Handles the /ads broadcast command for staff.
     */
    private boolean handleBroadcastCommand(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage(configManager.getCachedNoPermission());
            return true;
        }

        if (args.length < 4) {
            sender.sendMessage(Component.text("Usage: /ads broadcast <world|perm> <target> <message...>", NamedTextColor.RED));
            return true;
        }

        final String type = args[1].toLowerCase();
        final String target = args[2];
        final String adMessageRaw = String.join(" ", Arrays.copyOfRange(args, 3, args.length));

        final String validationError = validateAdMessage(adMessageRaw);
        if (validationError != null) {
            sender.sendMessage(legacySerializer.deserialize(validationError));
            return true;
        }

        final Component broadcastMessage = formatAdMessage(sender, adMessageRaw);

        switch (type) {
            case "world":
                handleWorldBroadcast(sender, target, broadcastMessage);
                break;
            case "perm":
                handlePermissionBroadcast(sender, target, broadcastMessage);
                break;
            default:
                sender.sendMessage(Component.text("Invalid broadcast type. Use 'world' or 'perm'.", NamedTextColor.RED));
        }
        return true;
    }

    /**
     * Finalizes the advertisement broadcast, applies cooldown, and removes the pending ad.
     */
    private boolean handleConfirm(final Player player) {
        final UUID playerUUID = player.getUniqueId();
        final String adMessageRaw = pendingAds.remove(playerUUID);

        if (adMessageRaw == null) {
            player.sendMessage(Component.text("You have no pending ad to confirm.", NamedTextColor.RED));
            return true;
        }

        final Component broadcastMessage = formatAdMessage(player, adMessageRaw);

        // --- FINAL FIX: Combine Console and Player collection using Stream.concat ---
        final Audience allRecipients = Audience.audience(
                Stream.concat(
                        Stream.of(Bukkit.getConsoleSender()), // Console Sender
                        Bukkit.getOnlinePlayers().stream()     // All Players
                ).collect(Collectors.toList())
        );

        allRecipients.sendMessage(broadcastMessage);
        allRecipients.playSound(configManager.getBroadcastSound());

        cooldownManager.applyCooldown(player);

        player.sendMessage(configManager.getCachedAdBroadcasted());

        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("DEBUG: Ad confirmed and broadcasted by " + player.getName());
        }
        return true;
    }

    /**
     * Handles the /ads cancel command.
     */
    private boolean handleCancel(final Player player) {
        if (pendingAds.remove(player.getUniqueId()) != null) {
            player.sendMessage(configManager.getCachedAdCancelled());
            if (configManager.isDebugEnabled()) {
                plugin.getLogger().info("DEBUG: Ad cancelled by " + player.getName());
            }
        } else {
            player.sendMessage(Component.text("You have no pending ad to cancel.", NamedTextColor.RED));
        }
        return true;
    }

    /**
     * Handles the /ads reload command.
     */
    private boolean handleReload(final CommandSender sender) {
        plugin.reloadConfig();
        plugin.getAdsConfigManager().setupConfig();
        sender.sendMessage(Component.text("KawaiiAD configuration reloaded.", NamedTextColor.GREEN));
        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("DEBUG: Config reloaded by " + sender.getName());
        }
        return true;
    }

    /**
     * Applies PAPI expansion and formatting to the raw message.
     */
    private Component formatAdMessage(final CommandSender sender, final String adMessageRaw) {
        final Component adPrefix = configManager.getCachedAdPrefix();
        String processedMessage = adMessageRaw;

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            final Player player = (sender instanceof Player p) ? p : null;
            processedMessage = PlaceholderAPI.setPlaceholders(player, processedMessage);
        }

        final Component messageComponent = legacySerializer.deserialize(processedMessage);
        return adPrefix.append(messageComponent);
    }

    private void handleWorldBroadcast(final CommandSender sender, final String worldName, final Component message) {
        final World world = Bukkit.getWorld(worldName);
        if (world == null) {
            sender.sendMessage(Component.text("World not found: " + worldName, NamedTextColor.RED));
            return;
        }
        if (!sender.hasPermission(BROADCAST_WORLD_PERMISSION)) {
            sender.sendMessage(configManager.getCachedNoPermission());
            return;
        }

        world.sendMessage(message);
        sender.sendMessage(Component.text("Broadcasted to world: " + worldName, NamedTextColor.GREEN));
    }

    private void handlePermissionBroadcast(final CommandSender sender, final String permission, final Component message) {
        Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission(permission))
                .forEach(p -> p.sendMessage(message));

        sender.sendMessage(Component.text("Broadcasted to players with permission: " + permission, NamedTextColor.GREEN));
    }

    private String validateAdMessage(final String message) {
        final int minChars = configManager.getMinLength();
        final int maxChars = configManager.getMaxLength();

        if (message.length() < minChars) {
            return configManager.getConfig().getString("messages.error-too-short", "&cAd must be at least %min% characters.")
                    .replace("%min%", String.valueOf(minChars));
        }
        if (message.length() > maxChars) {
            return configManager.getConfig().getString("messages.error-too-long", "&cAd must be no more than %max% characters.")
                    .replace("%max%", String.valueOf(maxChars));
        }

        for (final String word : configManager.getProfanityFilter()) {
            if (message.toLowerCase().contains(word.toLowerCase())) {
                return configManager.getConfig().getString("messages.error-profanity", "&cAdvertisement contains blacklisted words.");
            }
        }

        return null;
    }

    private void sendAdPreview(final Player player, final String adMessageRaw) {
        final long timeout = configManager.getConfirmationTimeoutSeconds();

        player.sendMessage(configManager.getCachedPreviewHeader());

        final Component adPrefix = configManager.getCachedAdPrefix();
        final Component previewAd = adPrefix.append(legacySerializer.deserialize(adMessageRaw));
        player.sendMessage(previewAd);

        player.sendMessage(configManager.getCachedPreviewFooter());

        final Component confirmButton = Component.text("[CONFIRM]", NamedTextColor.GREEN)
                .clickEvent(ClickEvent.runCommand("/ads confirm"))
                .hoverEvent(HoverEvent.showText(Component.text("Click to broadcast this ad.", NamedTextColor.GRAY)));

        final Component cancelButton = Component.text("[CANCEL]", NamedTextColor.RED)
                .clickEvent(ClickEvent.runCommand("/ads cancel"))
                .hoverEvent(HoverEvent.showText(Component.text("Click to discard this ad.", NamedTextColor.GRAY)));

        final Component prompt = Component.text("")
                .append(Component.text("Timeout: ", NamedTextColor.GRAY))
                .append(Component.text(timeout + "s", NamedTextColor.YELLOW))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(confirmButton)
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(cancelButton);

        player.sendMessage(prompt);
    }

    /**
     * Sends the remaining cooldown time to the player's Action Bar.
     */
    private void sendActionBarCooldown(final Player player, final long remainingSeconds) {
        final Component message = configManager.getMessage("on-cooldown",
                "<time_remaining>", cooldownManager.formatTime(remainingSeconds));

        // Start a short, repeating task to display the message
        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            private final int maxTicks = 60; // Display for 3 seconds (60 ticks)
            private int counter = 0;

            @Override
            public void run() {
                if (counter >= maxTicks || !player.isOnline()) {
                    player.sendActionBar(Component.empty());
                    throw new RuntimeException("Stop task");
                }
                player.sendActionBar(message);
                counter++;
            }
        }, 0L, 1L);
    }

    private void scheduleTimeout(final UUID playerUUID) {
        final long timeoutTicks = configManager.getConfirmationTimeoutSeconds() * 20L;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pendingAds.containsKey(playerUUID)) {
                pendingAds.remove(playerUUID);

                final Player player = Bukkit.getPlayer(playerUUID);
                if (player != null && player.isOnline()) {
                    player.sendMessage(configManager.getCachedAdTimeout());
                }
            }
        }, timeoutTicks);
    }
}