package dev.letsgoaway.geyserextras.core.locale;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import org.junit.jupiter.api.Test;

class GELocaleTest {
    @Test
    void commandHintsReplaceCachedDoubleTapInstructions() throws Exception {
        var field = GELocale.class.getDeclaredField("LANGUAGES_MAP");
        field.setAccessible(true);
        Object previous = field.get(null);
        try {
            var languages = new HashMap<String, HashMap<String, String>>();
            for (String locale : new String[]{"ja_JP", "en_US"}) {
                var translations = new HashMap<String, String>();
                translations.put("ge.welcome_toast.line2", "old double-tap hint");
                translations.put("ge.form_error_toast.line2", "old double-tap recovery");
                translations.put("unrelated.key", "keep this translation");
                languages.put(locale, translations);
            }
            field.set(null, languages);

            for (String locale : new String[]{"ja_JP", "en_US", "fr_FR"}) {
                for (String key : new String[]{"ge.welcome_toast.line2", "ge.form_error_toast.line2"}) {
                    String text = GELocale.translate(key, locale);
                    assertTrue(text.contains("/ge"));
                    assertFalse(text.contains("double-tap"));
                    assertFalse(text.contains("二回"));
                    assertEquals(text, GELocale.translateOtherwise(key, "old fallback", locale));
                }
            }
            assertEquals("/ge を入力してGeyserMenuを開く",
                    GELocale.translate("ge.welcome_toast.line2", "ja_JP"));
            assertEquals("keep this translation", GELocale.translate("unrelated.key", "en_US"));
        } finally {
            field.set(null, previous);
        }
    }
}
