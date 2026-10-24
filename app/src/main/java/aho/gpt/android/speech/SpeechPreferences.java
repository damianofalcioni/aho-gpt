package aho.gpt.android.speech;

import android.content.Context;
import aho.gpt.R;
import aho.gpt.core.SpeechLanguageOptions;
import java.util.Locale;

public final class SpeechPreferences {
    private static final String SETTINGS = "settings";
    public static final String INPUT = "speech_input_language";
    public static final String OUTPUT = "speech_output_language";
    public static final String VOICE = "speech_output_voice";

    private SpeechPreferences() { }

    public static boolean enabled(Context context, String key) {
        return context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE).getBoolean(key + "_enabled", true);
    }
    public static void enable(Context context, String key, boolean enabled) {
        context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE).edit().putBoolean(key + "_enabled", enabled).apply();
    }
    public static String voice(Context context) {
        return context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE).getString(VOICE, "");
    }
    public static void voice(Context context, String name) {
        context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE).edit().putString(VOICE, name).apply();
    }

    public static String selected(Context context, String key) {
        return SpeechLanguageOptions.normalize(context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE)
                .getString(key, ""));
    }
    public static void select(Context context, String key, String tag) {
        context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE).edit()
                .putString(key, SpeechLanguageOptions.normalize(tag)).apply();
    }
    public static Locale locale(Context context, String key) {
        String tag = selected(context, key);
        return tag.isEmpty() ? Locale.getDefault() : Locale.forLanguageTag(tag);
    }
    public static String label(Context context, Locale locale) {
        return context.getString(R.string.language_label, locale.getDisplayName(), locale.toLanguageTag());
    }
    public static String selectionLabel(Context context, String key) {
        String label = label(context, locale(context, key));
        return selected(context, key).isEmpty() ? context.getString(R.string.device_language, label) : label;
    }
}
