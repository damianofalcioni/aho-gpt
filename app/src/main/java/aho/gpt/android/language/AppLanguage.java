package aho.gpt.android.language;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;
import aho.gpt.core.AppLanguages;
import java.util.Locale;

/** Stores the manual choice independently of the system-language switch and speech. */
public final class AppLanguage {
    private static final String SYSTEM = "use_system_language";
    private static final String SELECTED = "app_language";
    private AppLanguage() { }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences("settings", Context.MODE_PRIVATE);
    }
    public static boolean useSystem(Context context) { return preferences(context).getBoolean(SYSTEM, true); }
    public static void useSystem(Context context, boolean enabled) {
        preferences(context).edit().putString(SELECTED, selected(context)).putBoolean(SYSTEM, enabled).apply();
    }
    public static String selected(Context context) {
        return AppLanguages.supported(preferences(context).getString(SELECTED, system()));
    }
    public static void select(Context context, String tag) {
        preferences(context).edit().putString(SELECTED, AppLanguages.supported(tag)).apply();
    }
    public static String effective(Context context) { return useSystem(context) ? system() : selected(context); }

    @android.annotation.SuppressLint("ObsoleteSdkInt") // Shared phone code supports Android 6.
    @SuppressWarnings("deprecation") // Configuration.locale is required on Android 6.
    private static String system() {
        Configuration configuration = Resources.getSystem().getConfiguration();
        return AppLanguages.match(Build.VERSION.SDK_INT >= 24
                ? configuration.getLocales().toLanguageTags() : configuration.locale.toLanguageTag());
    }
    @android.annotation.SuppressLint("ObsoleteSdkInt") // Shared phone code supports Android 6.
    public static Context wrap(Context context) {
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        Locale locale = Locale.forLanguageTag(effective(context));
        if (Build.VERSION.SDK_INT >= 24) { configuration.setLocales(new LocaleList(locale)); }
        else { configuration.setLocale(locale); }
        configuration.setLayoutDirection(locale);
        return context.createConfigurationContext(configuration);
    }
}
