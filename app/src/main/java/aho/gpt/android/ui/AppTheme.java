package aho.gpt.android.ui;

import android.app.Activity;
import android.content.Context;
import android.util.TypedValue;
import aho.gpt.R;

/** Saved display theme, independent of the device's appearance. */
public final class AppTheme {
    private static final String LIGHT = "light_theme";
    private AppTheme() { }

    public static boolean isLight(Context context) {
        return context.getSharedPreferences("settings", Context.MODE_PRIVATE).getBoolean(LIGHT, false);
    }
    public static void setLight(Context context, boolean enabled) {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putBoolean(LIGHT, enabled).apply();
    }
    public static int style(Context context) { return isLight(context) ? R.style.AppThemeLight : R.style.AppTheme; }
    public static boolean apply(Activity activity) {
        boolean light = isLight(activity);
        if (light) {
            TypedValue value = new TypedValue();
            activity.getTheme().resolveAttribute(R.attr.appLightTheme, value, true);
            activity.setTheme(value.resourceId);
        }
        return light;
    }
    public static int color(Context context, int attribute) {
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(attribute, value, true);
        return value.data;
    }
}
