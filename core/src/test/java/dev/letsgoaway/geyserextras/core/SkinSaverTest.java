package dev.letsgoaway.geyserextras.core;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import dev.letsgoaway.geyserextras.Server;
import dev.letsgoaway.geyserextras.core.config.GeyserExtrasConfig;
import dev.letsgoaway.geyserextras.core.features.skinsaver.SkinSaver;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.auth.BedrockClientData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class SkinSaverTest {
  static void set(Object o, String n, Object v) throws Exception {
    var f = o.getClass().getDeclaredField(n);
    f.setAccessible(true);
    f.set(o, v);
  }

  static void check(boolean b, String m) {
    if (!b) throw new AssertionError(m);
  }

  static class Fixture {
    Path root;
    Server server = mock(Server.class);
    GeyserSession session = mock(GeyserSession.class);
    ExtrasPlayer player = mock(ExtrasPlayer.class, CALLS_REAL_METHODS);
    BedrockClientData data = new BedrockClientData();

    Fixture(Path directory) throws Exception {
      root = Files.createDirectories(directory);
      GeyserExtras.SERVER = server;
      when(server.getPluginFolder()).thenReturn(root);
      when(session.getClientData()).thenReturn(data);
      player.session = session;
      doReturn(session).when(player).getSession();
      doReturn("1").when(player).getBedrockXUID();
      set(data, "skinId", "test-skin");
      set(data, "capeId", "test-cape");
      set(data, "skinImageWidth", 2);
      set(data, "skinImageHeight", 1);
      set(
          data,
          "skinData",
          new byte[] {(byte) 255, 0, 0, (byte) 255, 0, (byte) 255, 0, (byte) 128});
      set(data, "capeImageWidth", 1);
      set(data, "capeImageHeight", 1);
      set(data, "capeData", new byte[] {0, 0, (byte) 255, (byte) 255});
      set(data, "geometryName", "{\"geometry\":\"日本語\"}".getBytes(StandardCharsets.UTF_8));
      set(data, "geometryData", "{\"format_version\":\"1.12.0\"}".getBytes(StandardCharsets.UTF_8));
    }
  }

  @TempDir Path temporaryDirectory;

  @Test
  void regression() throws Exception {
    var f = new Fixture(temporaryDirectory.resolve("case-1"));
    String animation = "{\"animations\":{}}";
    set(
        f.data,
        "skinAnimationData",
        Base64.getEncoder().encodeToString(animation.getBytes(StandardCharsets.UTF_8)));
    SkinSaver.save(f.player);
    var image = ImageIO.read(f.root.resolve("skins/test-skin/skin.png").toFile());
    check(
        image.getWidth() == 2
            && image.getHeight() == 1
            && image.getRGB(0, 0) == 0xffff0000
            && image.getRGB(1, 0) == 0x8000ff00,
        "Raw RGBA and alpha must survive PNG encoding");
    check(
        Files.readString(f.root.resolve("skins/test-skin/skin.geometry_name.json")).contains("日本語"),
        "Geometry UTF-8 must survive");
    check(
        Files.readString(f.root.resolve("skins/test-skin/skin.animation.json")).equals(animation),
        "Animation string must be decoded once");
    check(
        ImageIO.read(f.root.resolve("capes/test-cape/cape.png").toFile()).getRGB(0, 0)
            == 0xff0000ff,
        "Cape pixels must survive");
    verify(f.server, never()).warn(anyString());
    System.out.println(
        "PASS: raw RGBA skin/cape, UTF-8 geometry and Base64 animation export correctly");
    f = new Fixture(temporaryDirectory.resolve("case-2"));
    set(f.data, "skinAnimationData", "bad!");
    SkinSaver.save(f.player);
    check(
        !Files.exists(f.root.resolve("skins/test-skin/skin.animation.json")),
        "Invalid animation must be skipped");
    check(
        Files.exists(f.root.resolve("capes/test-cape/cape.png")),
        "Invalid animation must not stop cape export");
    verify(f.server).warn(contains("skin.animation.json"));
    System.out.println("PASS: malformed animation does not interrupt other exports");
    f = new Fixture(temporaryDirectory.resolve("case-3"));
    set(f.data, "skinData", new byte[0]);
    set(f.data, "capeId", null);
    set(f.data, "capeData", null);
    set(f.data, "skinAnimationData", null);
    SkinSaver.save(f.player);
    check(
        Files.exists(f.root.resolve("skins/test-skin/skin.geometry.json")),
        "Empty optional image data must not stop geometry export");
    verify(f.server, never()).warn(anyString());
    System.out.println("PASS: empty/null optional data is handled");
    f = new Fixture(temporaryDirectory.resolve("case-4"));
    set(f.data, "skinData", new byte[3]);
    GeyserExtras.GE = mock(GeyserExtras.class);
    var config = new GeyserExtrasConfig();
    set(config, "enableSkinSaving", true);
    when(GeyserExtras.GE.getConfig()).thenReturn(config);
    doReturn("test").when(f.player).translateGE(anyString());
    doNothing().when(f.player).sendSystemToast(anyString(), anyString());
    UUID id = UUID.randomUUID();
    when(f.session.javaUuid()).thenReturn(id);
    f.player.startGame();
    check(
        f.player.isLoggedIn() && id.equals(f.player.getJavaUUID()),
        "Skin failure must not abort join initialization");
    check(
        Files.exists(f.root.resolve("skins/test-skin/skin.geometry.json")),
        "Invalid image must not block geometry export");
    verify(f.server).warn(contains("skin.png"));
    System.out.println("PASS: malformed image is skipped and startGame finishes");
  }
}
