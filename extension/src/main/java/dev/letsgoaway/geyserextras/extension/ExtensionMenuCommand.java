package dev.letsgoaway.geyserextras.extension;

import dev.letsgoaway.geyserextras.core.ExtrasPlayer;
import dev.letsgoaway.geyserextras.core.commands.GeyserExtrasCommand;
import org.geysermc.geyser.command.GeyserCommandSource;
import org.geysermc.geyser.extension.command.GeyserExtensionCommand;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;

import java.util.List;

import static dev.letsgoaway.geyserextras.core.GeyserExtras.GE;
import static org.incendo.cloud.parser.standard.StringParser.greedyStringParser;

/** Publishes the menu command to Geyser and the Bedrock command list. */
public final class ExtensionMenuCommand extends GeyserExtensionCommand {
    public ExtensionMenuCommand(GeyserExtrasExtension extension) {
        super(extension, "menu", "Open the GeyserPlus menu", "", null, true, true);
    }

    @Override
    public void register(CommandManager<GeyserCommandSource> manager) {
        manager.command(baseBuilder(manager)
                .optional("arguments", greedyStringParser())
                .handler(this::execute));
        // The extension API normally prefixes commands with /geyserextras.
        // Also register the documented /ge root, including a zero-argument form.
        manager.command(manager.commandBuilder("ge")
                .permission(commandPermission(manager))
                .optional("arguments", greedyStringParser())
                .handler(this::execute));
    }

    @Override
    public void execute(CommandContext<GeyserCommandSource> context) {
        var source = context.sender();
        var session = source.connection();
        if (session == null) {
            source.sendMessage("This command is available to connected Bedrock players only.");
            return;
        }
        session.ensureInEventLoop(() -> {
            if (GE == null) {
                source.sendMessage("GeyserPlus is still starting. Please try again shortly.");
                return;
            }
            ExtrasPlayer player = ExtrasPlayer.get(session);
            if (player == null) {
                source.sendMessage("GeyserPlus has not initialized this player yet. Please try again shortly.");
                return;
            }
            if (!GE.getConfig().isEnableGeyserExtrasMenu()) {
                source.sendMessage("The GeyserPlus menu is disabled in the server configuration.");
                return;
            }
            String arguments = context.<String>getOrDefault("arguments", "").trim();
            List<String> args = arguments.isEmpty() ? List.of() : List.of(arguments.split("\\s+"));
            new GeyserExtrasCommand().onExecute(player, args, "ge");
        });
    }
}
