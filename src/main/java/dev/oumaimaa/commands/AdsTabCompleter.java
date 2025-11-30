package dev.oumaimaa.commands;

import dev.oumaimaa.KawaiiAdPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provides tab completion for the /ads command based on current state and permissions.
 */
public final class AdsTabCompleter implements TabCompleter {

    private static final String ADMIN_PERMISSION = "kawaiid.admin";
    private final KawaiiAdPlugin plugin;

    /**
     * Constructs the TabCompleter.
     *
     * @param plugin The main plugin instance.
     */
    public AdsTabCompleter(final KawaiiAdPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String label, @NotNull final String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            return null;
        }

        if (args.length == 1) {
            Stream<String> options = Stream.empty();
            final String arg = args[0].toLowerCase();

            if (plugin.getPendingAds().containsKey(player.getUniqueId())) {
                options = Stream.of("confirm", "cancel");
            }

            if (player.hasPermission(ADMIN_PERMISSION)) {
                options = Stream.concat(options, Stream.of("reload", "broadcast"));
            }

            return options
                    .filter(s -> s.startsWith(arg))
                    .collect(Collectors.toList());

        } else if (args.length == 2 && args[0].equalsIgnoreCase("broadcast") && sender.hasPermission(ADMIN_PERMISSION)) {
            return Stream.of("world", "perm")
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length > 1) {
            return new ArrayList<>();
        }

        return null;
    }
}