package dev.letsgoaway.geyserextras.core;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import dev.letsgoaway.geyserextras.core.config.*;
import dev.letsgoaway.geyserextras.core.form.*;
import dev.letsgoaway.geyserextras.core.injectors.bedrock.*;
import dev.letsgoaway.geyserextras.core.preferences.*;
import dev.letsgoaway.geyserextras.core.preferences.bindings.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.cloudburstmc.protocol.bedrock.packet.InteractPacket;
import org.geysermc.cumulus.form.*;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.FormCache;
import org.junit.jupiter.api.Test;

public class MenuRecoveryTest {
  static class Task implements ScheduledFuture<Object> {
    Runnable work;
    boolean cancelled, done;

    Task(Runnable r) {
      work = r;
    }

    public boolean cancel(boolean b) {
      cancelled = true;
      return true;
    }

    public boolean isCancelled() {
      return cancelled;
    }

    public boolean isDone() {
      return done || cancelled;
    }

    public Object get() {
      return null;
    }

    public Object get(long n, TimeUnit u) {
      return null;
    }

    public long getDelay(TimeUnit u) {
      return 0;
    }

    public int compareTo(Delayed x) {
      return 0;
    }

    void run() {
      if (!cancelled) {
        done = true;
        work.run();
      }
    }
  }

  static class Fixture {
    GeyserSession session = mock(GeyserSession.class);
    ExtrasPlayer player = mock(ExtrasPlayer.class, CALLS_REAL_METHODS);
    PreferencesData prefs = mock(PreferencesData.class);
    FormCache cache = new FormCache(session);
    AtomicInteger menus = new AtomicInteger();
    List<Task> tasks = new ArrayList<>();
    BedrockInteractInjector injector = new BedrockInteractInjector();
    InteractPacket packet = new InteractPacket();

    Fixture() {
      GeyserExtras.GE = mock(GeyserExtras.class);
      GeyserExtras.GE.connections = new ConcurrentHashMap<>();
      GeyserExtras.GE.connections.put("1", player);
      when(GeyserExtras.GE.getConfig()).thenReturn(new GeyserExtrasConfig());
      player.session = session;
      doReturn(session).when(player).getSession();
      doReturn(prefs).when(player).getPreferences();
      when(session.xuid()).thenReturn("1");
      when(session.hasFormOpen()).thenAnswer(a -> cache.hasFormOpen());
      when(session.getFormCache()).thenReturn(cache);
      doAnswer(
              a -> {
                cache.closeForms();
                return null;
              })
          .when(session)
          .closeForm();
      when(session.sendForm(any(Form.class)))
          .thenAnswer(
              a -> {
                cache.addForm(a.getArgument(0));
                return true;
              });
      doAnswer(
              a -> {
                ((Runnable) a.getArgument(0)).run();
                return null;
              })
          .when(session)
          .ensureInEventLoop(any(Runnable.class));
      when(prefs.isEnableDoubleClickShortcut()).thenReturn(true);
      when(prefs.getDoubleClickMS()).thenReturn(200);
      when(session.scheduleInEventLoop(any(Runnable.class), anyLong(), any(TimeUnit.class)))
          .thenAnswer(
              a -> {
                var t = new Task(a.getArgument(0));
                tasks.add(t);
                return t;
              });
      doAnswer(
              a -> {
                menus.incrementAndGet();
                var m = ExtrasPlayer.class.getDeclaredMethod("sendGeyserForm", Form.class);
                m.setAccessible(true);
                m.invoke(player, SimpleForm.builder().title("test").build());
                return null;
              })
          .when(player)
          .sendForm(any(BedrockMenu.class));
      packet.setAction(InteractPacket.Action.OPEN_INVENTORY);
    }

    void tap() {
      injector.translate(session, packet);
    }

    void expireDebounce() throws Exception {
      var f = ExtrasPlayer.class.getDeclaredField("inventoryShortcutSuppressUntil");
      f.setAccessible(true);
      f.setLong(player, 0);
    }
  }

  static void check(boolean b, String why) {
    if (!b) throw new AssertionError(why);
  }

  @Test
  void floodgateFormsDoNotCrossGeyserClassLoader() throws Exception {
    var available = dev.letsgoaway.geyserextras.core.utils.IsAvailable.class
        .getDeclaredField("FLOODGATE");
    available.setAccessible(true);
    var holder = org.geysermc.floodgate.api.InstanceHolder.class.getDeclaredField("api");
    holder.setAccessible(true);
    var previousApi = holder.get(null);
    boolean previousAvailable = available.getBoolean(null);
    var previousType = dev.letsgoaway.geyserextras.ServerType.type;
    try {
      var api = mock(org.geysermc.floodgate.api.FloodgateApi.class);
      holder.set(null, api);
      available.setBoolean(null, true);
      dev.letsgoaway.geyserextras.ServerType.type = dev.letsgoaway.geyserextras.ServerType.VELOCITY;
      var f = new Fixture();
      var id = UUID.randomUUID();
      var uuid = ExtrasPlayer.class.getDeclaredField("javaUUID");
      uuid.setAccessible(true);
      uuid.set(f.player, id);
      f.player.sendForm(new BedrockMenu());
      verify(api).sendForm(eq(id), any(Form.class));
      verify(f.session, never()).sendForm(any(Form.class));
      check(f.player.getInventoryShortcutSuppressUntil() > 0, "Keep input debounce on Floodgate");
      dev.letsgoaway.geyserextras.ServerType.type = dev.letsgoaway.geyserextras.ServerType.EXTENSION;
      f.player.sendForm(new BedrockMenu());
      verify(f.session).sendForm(any(Form.class));
    } finally {
      holder.set(null, previousApi);
      available.setBoolean(null, previousAvailable);
      dev.letsgoaway.geyserextras.ServerType.type = previousType;
    }
  }

  @Test
  void regression() throws Exception {
    var f = new Fixture();
    f.cache.addForm(CustomForm.builder().title("server settings").build());
    f.tap();
    check(
        !f.cache.hasFormOpen() && f.tasks.size() == 1,
        "Leaving settings must clear cached form and accept inventory input");
    f.tasks.get(0).run();
    verify(f.prefs).runAction(Remappable.OPEN_INVENTORY);
    System.out.println("PASS: actual cached settings form does not block inventory");
    f = new Fixture();
    f.tap();
    f.player.setLastInventoryClickTime(TimeUnit.NANOSECONDS.toMillis(System.nanoTime()) - 100);
    f.tap();
    check(f.menus.get() == 1 && f.cache.hasFormOpen(), "Double tap must queue one menu");
    f.tap();
    f.tap();
    check(f.menus.get() == 1 && f.tasks.size() == 1, "Immediate extra taps should be debounced");
    f.expireDebounce();
    f.tap();
    check(
        !f.cache.hasFormOpen() && f.tasks.size() == 2,
        "An unanswered form must not permanently lock inventory");
    f.tasks.get(1).run();
    verify(f.prefs).runAction(Remappable.OPEN_INVENTORY);
    System.out.println(
        "PASS: double tap opens once; extra taps are bounded; unanswered form recovers");
    f = new Fixture();
    f.tap();
    f.cache.addForm(CustomForm.builder().title("settings arrived while click pending").build());
    f.tasks.get(0).run();
    check(!f.cache.hasFormOpen(), "Pending click must recover stale settings");
    verify(f.prefs).runAction(Remappable.OPEN_INVENTORY);
    System.out.println("PASS: settings cached during a pending click cannot swallow that click");
    f = new Fixture();
    when(f.prefs.isEnableDoubleClickShortcut()).thenReturn(false);
    when(f.prefs.isDefault(Remappable.OPEN_INVENTORY)).thenReturn(true);
    f.cache.addForm(
        CustomForm.builder()
            .title("settings")
            .build()); // Normal translator opens inventory; verify forms are released before
                       // delegation.
    // Keep this test on the remapped-action branch so no live entity/inventory is required.
    when(f.prefs.isDefault(Remappable.OPEN_INVENTORY)).thenReturn(false);
    when(f.prefs.getAction(Remappable.OPEN_INVENTORY)).thenReturn(Action.DISABLED);
    // The important state release happens before the remapping path.
    when(f.session.getPlayerEntity())
        .thenReturn(mock(org.geysermc.geyser.entity.type.player.SessionPlayerEntity.class));
    f.tap();
    check(!f.cache.hasFormOpen(), "Disabled shortcut must still release settings form");
    System.out.println("PASS: settings recovery also applies with shortcut disabled");
  }
}
