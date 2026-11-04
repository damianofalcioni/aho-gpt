package aho.gpt.core;

import java.util.Locale;

/** Matches supported display languages without changing speech preferences. */
public final class AppLanguages {
    private AppLanguages() { }

    public static String supported(String tag) {
        return "it".equals(Locale.forLanguageTag(tag == null ? "" : tag).getLanguage()) ? "it" : "en";
    }
    public static String match(String tags) {
        for (String tag : tags.split(",")) {
            String language = Locale.forLanguageTag(tag).getLanguage();
            if ("en".equals(language) || "it".equals(language)) { return language; }
        }
        return "en";
    }
}
