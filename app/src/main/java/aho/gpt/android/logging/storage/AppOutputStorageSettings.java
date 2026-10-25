package aho.gpt.android.logging.storage;

import android.content.Context;

/** Installation-local SD selection; folder grants remain with Android. */
public final class AppOutputStorageSettings {
    private static final String PREFS = "output_folders";

    private AppOutputStorageSettings() { }

    public static boolean useSdCard(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("use_sd_card", false);
    }

    public static String cardId(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("sd_card_id", "");
    }

    public static void select(Context context, String id) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("sd_card_id", id)
                .putBoolean("use_sd_card", true).apply();
    }

    public static void disable(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("use_sd_card", false).apply();
    }
}
