package dev.oumaimaa.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * Executes the /kawaiiadshelp command, showing a rich, interactive help menu.
 */
public final class HelpCommand implements CommandExecutor {

    private static final Component HEADER = Component.text("--- KawaiiAD Help ---", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD);
    private static final Component FOOTER = Component.text("-----------------------", NamedTextColor.LIGHT_PURPLE);

    @Override
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String label, @NotNull final String @NotNull [] args) {
        sender.sendMessage(HEADER);

        sender.sendMessage(createHelpLine("/ads <message>", "Submit an ad for confirmation.", "kawaiid.use"));

        sender.sendMessage(createHelpLine("/ads confirm", "Confirm your pending ad.", "kawaiid.use"));
        sender.sendMessage(createHelpLine("/ads cancel", "Cancel your pending ad.", "kawaiid.use"));

        if (sender.hasPermission("kawaiid.admin")) {
            sender.sendMessage(Component.text("--- Admin Commands ---", NamedTextColor.GOLD));
            sender.sendMessage(createHelpLine("/ads reload", "Reloads the configuration.", "kawaiid.admin"));
            sender.sendMessage(createHelpLine("/ads broadcast", "Send an immediate, non-cooldown ad.", "kawaiid.admin"));
        }

        sender.sendMessage(Component.text("PAPI: %kawaiiads_cooldown_remaining%", NamedTextColor.AQUA)
                .hoverEvent(HoverEvent.showText(Component.text("Shows your time left on cooldown.", NamedTextColor.GRAY))));


        sender.sendMessage(FOOTER);
        return true;
    }

    /**
     * Creates a formatted, clickable component line for the help menu.
     */
    private @NotNull Component createHelpLine(final String cmd, final String description, final String permission) {
        final Component commandText = Component.text(cmd, NamedTextColor.GREEN)
                .clickEvent(ClickEvent.suggestCommand(cmd));

        final Component descriptionText = Component.text(" - " + description, NamedTextColor.GRAY)
                .hoverEvent(HoverEvent.showText(Component.text("Permission: ", NamedTextColor.YELLOW).append(Component.text(permission))));

        return commandText.append(descriptionText);
    }
}