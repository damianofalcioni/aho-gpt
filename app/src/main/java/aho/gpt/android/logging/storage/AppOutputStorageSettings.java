package aho.gpt.android.logging.storage;

import android.content.Context;

/** Installation-local SD selection and grant; never restored as portable app settings. */
public final class AppOutputStorageSettings {
    private static final String PREFS = "output_folders";

    private AppOutputStorageSettings() { }

    public static boolean useSdCard(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("use_sd_card", false);
    }

    public static String cardId(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("sd_card_id", "");
    }

    public static String tree(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("sd_download_tree", null);
    }

    public static void select(Context context, String id, String tree) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("sd_card_id", id).putString("sd_download_tree", tree)
                .putBoolean("use_sd_card", true).apply();
    }

    public static void disable(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("use_sd_card", false).apply();
    }
}
