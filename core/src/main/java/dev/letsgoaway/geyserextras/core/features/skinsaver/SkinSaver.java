package dev.letsgoaway.geyserextras.core.features.skinsaver;

import dev.letsgoaway.geyserextras.core.ExtrasPlayer;
import org.geysermc.geyser.session.auth.BedrockClientData;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Objects;

import static dev.letsgoaway.geyserextras.core.GeyserExtras.SERVER;

public class SkinSaver {
    public static void save(ExtrasPlayer player) {
        try {
            BedrockClientData data = player.getSession().getClientData();
            String capeId = Objects.requireNonNullElse(data.getCapeId(), "");
            String skinId = Objects.requireNonNullElse(data.getSkinId(), "skin");
            if (!capeId.isEmpty()) {
                skinId = skinId.replace(capeId, "");
            }
            if (skinId.isBlank()) {
                skinId = "skin";
            }
            Path skinsFolder = Files.createDirectories(SERVER.getPluginFolder().resolve("skins").resolve(skinId));

            // Geyser 2.12.1's byte[] fields are already Base64-decoded by
            // BedrockClientData.StringToByteDeserializer. Never decode them twice.
            saveImage(skinsFolder.resolve("skin.png"), data.getSkinImageWidth(),
                    data.getSkinImageHeight(), data.getSkinData(), player);
            saveText(skinsFolder.resolve("skin.geometry.json"), data.getGeometryData(), player);
            saveText(skinsFolder.resolve("skin.geometry_name.json"), data.getGeometryName(), player);

            // Animation data remains a Base64-encoded String in BedrockClientData.
            String animation = data.getSkinAnimationData();
            if (animation != null && !animation.isBlank()) {
                try {
                    saveText(skinsFolder.resolve("skin.animation.json"), Base64.getDecoder().decode(animation), player);
                } catch (IllegalArgumentException e) {
                    warn(player, "skin.animation.json", e);
                }
            }

            if (!capeId.isEmpty() && data.getCapeData() != null && data.getCapeData().length != 0) {
                Path capesFolder = Files.createDirectories(SERVER.getPluginFolder().resolve("capes").resolve(capeId));
                saveImage(capesFolder.resolve("cape.png"), data.getCapeImageWidth(),
                        data.getCapeImageHeight(), data.getCapeData(), player);
            }
        } catch (IOException | RuntimeException e) {
            // Skin export is optional; failure must not abort SessionJoinEvent.
            warn(player, "skin data", e);
        }
    }

    private static void saveImage(Path path, int width, int height, byte[] pixels, ExtrasPlayer player) {
        if (pixels == null || pixels.length == 0) {
            return;
        }
        try {
            if (!ImageIO.write(getFromSkinData(width, height, pixels), "png", path.toFile())) {
                throw new IOException("PNG writer unavailable");
            }
        } catch (IOException | IllegalArgumentException e) {
            warn(player, path.getFileName().toString(), e);
        }
    }

    private static void saveText(Path path, byte[] bytes, ExtrasPlayer player) {
        if (bytes == null || bytes.length == 0) {
            return;
        }
        try {
            Files.writeString(path, new String(bytes, StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        } catch (IOException e) {
            warn(player, path.getFileName().toString(), e);
        }
    }

    private static void warn(ExtrasPlayer player, String component, Exception error) {
        SERVER.warn("Could not save " + component + " for player " + player.getBedrockXUID()
                + ", " + error.getMessage());
    }

    private static BufferedImage getFromSkinData(int width, int height, byte[] pixelData) {
        if (width <= 0 || height <= 0 || (long) width * height * 4 != pixelData.length) {
            throw new IllegalArgumentException("Invalid RGBA image dimensions or pixel data length");
        }
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = new int[width * height];
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = ((pixelData[i * 4 + 3] & 0xFF) << 24)
                    | ((pixelData[i * 4] & 0xFF) << 16)
                    | ((pixelData[i * 4 + 1] & 0xFF) << 8)
                    | (pixelData[i * 4 + 2] & 0xFF);
        }
        image.setRGB(0, 0, width, height, pixels, 0, width);
        return image;
    }
}
