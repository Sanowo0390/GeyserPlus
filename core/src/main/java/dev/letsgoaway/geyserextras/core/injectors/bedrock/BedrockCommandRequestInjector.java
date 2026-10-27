package dev.letsgoaway.geyserextras.core.injectors.bedrock;

import dev.letsgoaway.geyserextras.core.commands.CommandExecutor;
import dev.letsgoaway.geyserextras.core.injectors.GeyserHandler;
import org.cloudburstmc.protocol.bedrock.packet.CommandRequestPacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.translator.protocol.bedrock.BedrockCommandRequestTranslator;
import dev.letsgoaway.geyserextras.core.ExtrasPlayer;

@Translator(packet = CommandRequestPacket.class)
public class BedrockCommandRequestInjector extends BedrockCommandRequestTranslator {
    @Override
    public void translate(GeyserSession session, CommandRequestPacket packet) {
        String input = packet.getCommand().trim();
        String root = input.startsWith("/") ? input.substring(1).split("\\s+", 2)[0] : "";
        if (root.equalsIgnoreCase("ge") || root.equalsIgnoreCase("geyserextras")) {
            // These are now real Geyser commands. Let its command registry dispatch
            // them instead of bypassing native argument parsing and permissions.
            super.translate(session, packet);
            return;
        }
        if (CommandExecutor.isExtrasCommand(packet.getCommand())) {
            ExtrasPlayer player = ExtrasPlayer.get(session);
            CommandExecutor.runFromInput(player, packet.getCommand());
        } else {
            super.translate(session, packet);
        }
    }
}
