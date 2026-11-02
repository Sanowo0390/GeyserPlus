package dev.letsgoaway.geyserextras.core.config;

import lombok.Getter;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("FieldMayBeFinal")
@ConfigSerializable
@Getter

// TODO make wiki page/s to explain stuff better
public final class GeyserExtrasConfig {
    @Comment("""
            Enable the GeyserExtras menu, which players can open by typing `/ge`.
            The optional double-inventory shortcut is disabled by default (0 ms).
            \s
            The GeyserExtras Menu provides actions and configurable settings such as
            - Remapping certain actions (e.g emotes/pick block) to Java Edition only actions (e.g offhand)
            - Configurable HUD Visibility
            - Quick access to form remakes of Java Edition Menus
              (e.g Player List, Advancements, Statistics).
            \s
            You can configure default options for Bedrock Players at
            `preferences/default.json`
            and for Java Players at
            `preferences/java/default.json`.
            \s
            Setting this to `false` also disables saving of preferences.
            """)
    private boolean enableGeyserExtrasMenu = true;

    @Comment("Enable the Java Edition combat sounds.")
    private boolean enableJavaCombatSounds = true;

    @Comment("""
            Enable the Block Display entity workaround using FMBE. (EXPERIMENTAL)
            Note that the translation is not 100% accurate and some block types might be invisible (candles, doors, campfires),
            where as some blocks will render differently (fence, glass panes, some storage containers).
            Block Display billboards do not work and X/Z scale are linked to which ever one is highest,
            meaning that this should only be enabled for very simple usages of Block Displays.
            (https://wiki.bedrock.dev/commands/display-entities.html)
            """)
    private boolean enableBlockDisplayWorkaround = false;


    @Comment("""
            Requires PacketEvents and Java Edition clients on 1.21.9 or above.
            https://modrinth.com/plugin/packetevents

            Enables the cape workaround. (EXPERIMENTAL)
            
            This shows Bedrock Edition player capes on Java Edition clients,
            by using fake mannequin player entities.
            It copies the packets of the player, so the position is identical.
            The hitbox of the mannequin is identical to the player and
            whenever the mannequin is attacked by the player it is translated as an attack
            to the bedrock player.
            
            This may break anti-cheats or plugins, and is not perfect.
            
            The Bedrock Edition player's cape will sometimes slightly clip
            inside the Bedrock Player when wearing armor and if the player attacks.
            
            The cape is visually removed when entering mounts that can support more than one
            player (including chest boats).
            
            This only works with capes that are available on Java Edition, that are also on
            Bedrock Edition.
            """)
    private boolean enableBedrockCapesOnJavaWorkaround = true;

    @Comment("When the server closes or Geyser reloads, should GeyserExtras automatically reconnect players to the server.")
    private boolean autoReconnect = true;

    @Comment(
            """
            The language that Java Edition players read when using a GeyserExtras command (e.g, `/muteemotechat`).
            This is automatically set on Bedrock Edition from the player's settings.
            You can see the list of all locale codes here:
            https://github.com/GeyserExtras/data/blob/main/langs/language_names.json
            """)
    private String defaultLocale = "en_US";

    @Comment("""
            Enables downloading of Bedrock Player skins.
            This will download the Geometry, Animations, and Textures of your players skins/capes.
            Persona skins are currently bugged.
            """)
    private boolean enableSkinSaving = false;


    @Comment("""
            Whether to disable the Paper Doll or not.
            The Paper Doll is the small player graphic in the top left of bedrock players screens
            This can give players a competitive advantage over java in some cases, for example,
            you can see what armor you have on without pressing the inventory key
            you can see what armor has broken during combat
            """)
    private boolean disablePaperDoll = false;

    @Comment("""
            Whether to check for updates or not.
            This also disables updating of the GeyserExtrasPack,
            however you can manually update them by deleting the 'GeyserExtras/cache/' folder.
            """)
    private boolean checkForUpdates = true;

    @Comment("""
            The priority level for the GeyserExtrasPack resource pack.
            Higher values = higher priority (packs with higher priority override files from lower-priority packs
            when file names conflict, and the client cannot easily merge them).
            Valid range: -100 to 100
            Common values:
            - -100: Lowest priority
            - 0: Normal priority
            - 100: Highest priority (default)

            Only change this if you have other resource packs that need to override GeyserExtrasPack,
            or if GeyserExtrasPack needs lower priority than your custom packs.

            Note: If you serve GeyserExtrasPack externally via CDN (using UrlPackCodec),
            GeyserExtras will automatically detect it by UUID and skip registering its own copy.
            """)
    private int geyserExtrasPackPriority = 100;

    @Comment("Only enable if you know what you are doing.")
    private boolean debugMode = false;

    @Comment("""
            Owner-facing menu customization. Titles left blank keep GeyserExtras' translated default.
            Menu button keys: swap-offhand, reconnect, settings, advancements, statistics, player-list,
            server-links, resource-packs. Set enabled=false to hide an entry. Set disabled=true to show
            the configured disabled label/icon without running its action. sort-order controls button order.
            Image types are PATH, URL, or NONE. URL icons must be reachable by the Bedrock client.
            """)
    private MenuSettings menus = new MenuSettings();

    public MenuSettings menuSettings() {
        if (menus == null) menus = new MenuSettings();
        return menus;
    }

    public String menuTitle(String key, String fallback) {
        return menuSettings().title(key, fallback);
    }

    public String menuContent(String key, String fallback) {
        return menuSettings().content(key, fallback);
    }

    public String textOverride(String key, String fallback) {
        return menuSettings().text(key, fallback);
    }

    @ConfigSerializable
    @Getter
    public static final class MenuSettings {
        @Comment("Override menu titles by key. Supported keys include main, settings, settings.ingame, packs, server-links, player-list.")
        private Map<String, String> titleOverrides = new LinkedHashMap<>();

        @Comment("Main menu description/content. Leave blank to keep it empty.")
        private String mainContent = "";

        @Comment("Content/header overrides by menu key. Missing or blank entries preserve the built-in text.")
        private Map<String, String> contentOverrides = new LinkedHashMap<>();

        @Comment("Overrides any string passed through ExtrasPlayer.translate/translateGE, keyed by its translation key.")
        private Map<String, String> textOverrides = new LinkedHashMap<>();

        @Comment("Main menu button overrides. Missing entries use built-in defaults.")
        private Map<String, MenuButtonConfig> buttons = defaultButtons();

        public String title(String key, String fallback) {
            if (titleOverrides == null) return fallback;
            String value = titleOverrides.get(key);
            return value == null || value.isBlank() ? fallback : value;
        }

        public String content(String key, String fallback) {
            if (contentOverrides != null) {
                String value = contentOverrides.get(key);
                if (value != null && !value.isBlank()) return value;
            }
            return fallback == null ? "" : fallback;
        }

        public String text(String key, String fallback) {
            if (textOverrides != null) {
                String value = textOverrides.get(key);
                if (value != null && !value.isBlank()) return value;
            }
            return fallback == null ? "" : fallback;
        }

        public MenuButtonConfig button(String key, int fallbackOrder, String fallbackImageType, String fallbackImage) {
            MenuButtonConfig value = buttons == null ? null : buttons.get(key);
            return value == null ? MenuButtonConfig.defaults(fallbackOrder, fallbackImageType, fallbackImage) : value;
        }

        private static Map<String, MenuButtonConfig> defaultButtons() {
            Map<String, MenuButtonConfig> values = new LinkedHashMap<>();
            values.put("swap-offhand", MenuButtonConfig.defaults(10, "PATH", "textures/ui/move.png"));
            values.put("reconnect", MenuButtonConfig.defaults(20, "PATH", "textures/ui/refresh_hover.png"));
            values.put("settings", MenuButtonConfig.defaults(30, "PATH", "textures/ui/settings_glyph_color_2x.png"));
            values.put("advancements", MenuButtonConfig.defaults(40, "PATH", "textures/ui/achievements.png"));
            values.put("statistics", MenuButtonConfig.defaults(50, "PATH", "textures/ui/world_glyph_color_2x_black_outline.png"));
            values.put("player-list", MenuButtonConfig.defaults(60, "PATH", "textures/ui/Local.png"));
            values.put("server-links", MenuButtonConfig.defaults(70, "PATH", "textures/ui/external_link.png"));
            values.put("resource-packs", MenuButtonConfig.defaults(80, "PATH", "textures/ui/glyph_resource_pack.png"));
            return values;
        }
    }

    @ConfigSerializable
    @Getter
    public static final class MenuButtonConfig {
        @Comment("Set false to hide this button completely.")
        private boolean enabled = true;
        @Comment("Show the button as disabled. Its normal action will not run.")
        private boolean disabled = false;
        @Comment("Show a disabled placeholder when the feature is unavailable (for example, no server links exist).")
        private boolean showWhenUnavailable = false;
        @Comment("Sort order; lower numbers appear first.")
        private int sortOrder = Integer.MIN_VALUE;
        @Comment("Label override. Blank keeps the translated built-in label.")
        private String title = "";
        @Comment("Icon type: PATH, URL, or NONE. Blank keeps the built-in icon type.")
        private String imageType = "";
        @Comment("Icon value/path/URL. Blank keeps the built-in icon.")
        private String image = "";
        @Comment("Label shown while disabled. Blank generates a disabled label from the normal title.")
        private String disabledTitle = "";
        @Comment("Disabled icon type: PATH, URL, or NONE. Blank keeps the normal icon type.")
        private String disabledImageType = "";
        @Comment("Disabled icon value/path/URL. Blank keeps the normal icon.")
        private String disabledImage = "";
        @Comment("Message sent when a disabled button is clicked. Blank makes the disabled button silent.")
        private String disabledMessage = "This option is currently unavailable.";

        private static MenuButtonConfig defaults(int order, String imageType, String image) {
            MenuButtonConfig value = new MenuButtonConfig();
            value.sortOrder = order;
            value.imageType = imageType;
            value.image = image;
            return value;
        }
    }

    @Comment("The version of the config. DO NOT CHANGE!")
    private int version = ConfigLoader.LATEST_VERSION;
}
