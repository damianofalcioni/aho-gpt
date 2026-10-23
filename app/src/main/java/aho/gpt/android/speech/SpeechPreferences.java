package aho.gpt.android.speech;

import android.content.Context;
import aho.gpt.R;
import aho.gpt.core.SpeechLanguageOptions;
import java.util.Locale;

public final class SpeechPreferences {
    public static final String INPUT = "speech_input_language";
    public static final String OUTPUT = "speech_output_language";

    private SpeechPreferences() { }

    public static String selected(Context context, String key) {
        return SpeechLanguageOptions.normalize(context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                .getString(key, ""));
    }
    public static void select(Context context, String key, String tag) {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit()
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
