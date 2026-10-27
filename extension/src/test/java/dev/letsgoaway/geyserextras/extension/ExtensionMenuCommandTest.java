package dev.letsgoaway.geyserextras.extension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import dev.letsgoaway.geyserextras.core.*;
import dev.letsgoaway.geyserextras.core.config.*;
import dev.letsgoaway.geyserextras.core.form.*;
import dev.letsgoaway.geyserextras.core.injectors.bedrock.*;
import java.util.*;
import java.util.concurrent.*;
import org.cloudburstmc.protocol.bedrock.packet.CommandRequestPacket;
import org.geysermc.geyser.command.GeyserCommandSource;
import org.geysermc.geyser.session.GeyserSession;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.internal.CommandRegistrationHandler;
import org.junit.jupiter.api.Test;

public class ExtensionMenuCommandTest {
  static class Manager extends CommandManager<GeyserCommandSource> {
    Manager() {
      super(
          ExecutionCoordinator.simpleCoordinator(),
          CommandRegistrationHandler.nullCommandRegistrationHandler());
    }

    public boolean hasPermission(GeyserCommandSource s, String p) {
      return true;
    }
  }

  static void check(boolean b, String m) {
    if (!b) throw new AssertionError(m);
  }

  @Test
  void regression() throws Exception {
    var locale = org.geysermc.geyser.text.GeyserLocale.class.getDeclaredField("DEFAULT_LOCALE");
    locale.setAccessible(true);
    locale.set(null, "en_US");
    var mappings = org.geysermc.geyser.text.GeyserLocale.class.getDeclaredField("LOCALE_MAPPINGS");
    mappings.setAccessible(true);
    ((java.util.Map) mappings.get(null)).put("en_US", new java.util.Properties());
    var extension = mock(GeyserExtrasExtension.class);
    when(extension.rootCommand()).thenReturn("geyserextras");
    var command = new ExtensionMenuCommand(extension);
    var manager = new Manager();
    command.register(manager);
    check(
        manager.rootCommands().contains("ge") && manager.rootCommands().contains("geyserextras"),
        "Both roots must be registered");
    var session = mock(GeyserSession.class);
    when(session.xuid()).thenReturn("1");
    doAnswer(
            i -> {
              ((Runnable) i.getArgument(0)).run();
              return null;
            })
        .when(session)
        .ensureInEventLoop(any(Runnable.class));
    var source = mock(GeyserCommandSource.class);
    when(source.connection()).thenReturn(session);
    when(source.isConsole()).thenReturn(false);
    GeyserExtras.GE = mock(GeyserExtras.class);
    when(GeyserExtras.GE.getConfig()).thenReturn(new GeyserExtrasConfig());
    GeyserExtras.GE.connections = new ConcurrentHashMap<>();
    var player = mock(ExtrasPlayer.class);
    when(player.getSession()).thenReturn(session);
    GeyserExtras.GE.connections.put("1", player);
    manager.commandExecutor().executeCommand(source, "ge").join();
    verify(player, times(1)).sendForm(any(BedrockMenu.class));
    System.out.println("PASS: native /ge with zero arguments dispatches the menu");
    manager.commandExecutor().executeCommand(source, "ge settings").join();
    verify(player, times(2)).sendForm(any(BedrockMenu.class));
    System.out.println("PASS: native /ge settings dispatches a form");
    manager.commandExecutor().executeCommand(source, "geyserextras menu").join();
    verify(player, times(3)).sendForm(any(BedrockMenu.class));
    System.out.println("PASS: native /geyserextras menu dispatches the menu");
    var packet = new CommandRequestPacket();
    packet.setCommand("/ge");
    new BedrockCommandRequestInjector().translate(session, packet);
    verify(session).sendCommand("ge");
    System.out.println("PASS: Bedrock /ge packets are delegated to native Geyser dispatch");
    System.out.println("PASS: roots present in real command tree: " + manager.rootCommands());
  }
}
