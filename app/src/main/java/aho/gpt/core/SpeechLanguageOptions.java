package aho.gpt.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IllformedLocaleException;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/** Unknown provider support may use device locales; an explicitly empty catalog may not. */
public final class SpeechLanguageOptions {
    private SpeechLanguageOptions() { }

    public static String normalize(String tag) {
        if (tag == null || tag.trim().isEmpty()) { return ""; }
        try {
            Locale locale = new Locale.Builder().setLanguageTag(tag.trim().replace('_', '-')).build();
            return locale.getLanguage().isEmpty() ? "" : locale.toLanguageTag();
        } catch (IllformedLocaleException exception) { return ""; }
    }

    public static List<String> choices(Collection<String> reported, Collection<Locale> deviceLocales) {
        Collection<String> source = reported;
        if (source == null) {
            source = new ArrayList<>();
            for (Locale locale : deviceLocales) { source.add(locale.toLanguageTag()); }
        }
        TreeSet<String> tags = new TreeSet<>();
        for (String tag : source) {
            String normalized = normalize(tag);
            if (!normalized.isEmpty()) { tags.add(normalized); }
        }
        return new ArrayList<>(tags);
    }
}
