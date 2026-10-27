package dev.letsgoaway.geyserextras.core.locale;

import com.google.gson.JsonObject;
import dev.letsgoaway.geyserextras.core.cache.Cache;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class GELocale {
    private static HashMap<String, HashMap<String, String>> LANGUAGES_MAP = new HashMap<>();

    public static void loadAll() {
        try {
            FileInputStream data = new FileInputStream(Cache.LANGUAGE_FOLDER.resolve("language_names.json").toFile());

            String langNames = new String(data.readAllBytes(), StandardCharsets.UTF_8);
            String[][] langs = Cache.GSON.fromJson(langNames, String[][].class);
            for (String[] lang : langs) {
                FileInputStream langData = new FileInputStream(Cache.LANGUAGE_FOLDER.resolve(lang[0] + ".json").toFile());
                JsonObject json = Cache.GSON.fromJson(new String(langData.readAllBytes(), StandardCharsets.UTF_8), JsonObject.class);
                HashMap<String, String> languageData = new HashMap<>();
                for (String key : json.keySet()) {
                    String translation = json.get(key).getAsString();

                    while (translation.contains("/{") && translation.contains("}/")) {
                        for (String key2 : json.keySet()) {
                            if (translation.contains("/{" + key2 + "}/")) {
                                translation = translation.replace("/{" + key2 + "}/", json.get(key2).getAsString());
                            }
                        }
                    }

                    languageData.put(key, translation);
                }
                LANGUAGES_MAP.put(lang[0], languageData);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String translate(String key, String locale) {
        String corrected = japaneseMenuTranslation(key, locale);
        if (corrected != null) return corrected;
        String fallback = builtInTranslation(key, locale);
        if (LANGUAGES_MAP.containsKey(locale)) {
            return LANGUAGES_MAP.get(locale).getOrDefault(key, fallback);
        }
        return LANGUAGES_MAP.getOrDefault("en_US", new HashMap<>()).getOrDefault(key, fallback);
    }

    // Downloaded language caches may predate these settings. Keep labels in the
    // plugin so existing installations do not need to delete their language cache.
    private static String builtInTranslation(String key, String locale) {
        boolean japanese = locale != null && locale.toLowerCase(java.util.Locale.ROOT).startsWith("ja");
        return switch (key) {
            case "ge.settings.section.submit_warning" -> japanese
                    ? "設定を保存するには、各設定画面の「送信」を押してください。"
                    : "Press Submit on each settings screen to save your changes.";
            case "ge.settings.menu.sendSystemToasts" -> japanese
                    ? "システム通知を表示する"
                    : "Show system notifications";
            default -> key;
        };
    }

    public static String translateOtherwise(String key, String otherwise, String locale) {
        String corrected = japaneseMenuTranslation(key, locale);
        if (corrected != null) return corrected;
        if (LANGUAGES_MAP.containsKey(locale)) {
            return LANGUAGES_MAP.get(locale).getOrDefault(key, key);
        }

        return otherwise;
    }

    // Override outdated downloaded labels, including already cached translations.
    private static String japaneseMenuTranslation(String key, String locale) {
        if (locale == null || !locale.toLowerCase(java.util.Locale.ROOT).startsWith("ja")) return null;
        return switch (key) {
            case "ge.welcome_toast.line2", "ge.settings.menu.doubleClickInventoryShortcut" ->
                    "インベントリを素早く二回開いてメニューを開く";
            case "ge.form_error_toast.line2" ->
                    "ゲーム内でインベントリを素早く二回開いて設定メニューを開くことができます";
            case "ge.settings.menu.doubleClickTime" -> "インベントリを二回開く間隔（ミリ秒）";
            default -> null;
        };
    }
}
