package aho.gpt.android.tools;

import android.content.Context;
import android.content.SharedPreferences;

/** Stores user choices only; discovered app metadata remains in memory. */
public final class ToolPreferences {
    private static final String ENABLED = "enabled";
    private static final String VERSION = "version";
    private final SharedPreferences preferences;

    public ToolPreferences(Context context) {
        preferences = context.getSharedPreferences("tools", Context.MODE_PRIVATE);
    }
    public boolean enabled() { return preferences.getBoolean(ENABLED, true); }
    public void enable(boolean enabled) { select(ENABLED, enabled); }
    public boolean selected(String key) { return preferences.getBoolean(key, true); }
    public void select(String key, boolean enabled) {
        synchronized (preferences) {
            if (selected(key) == enabled) { return; }
            preferences.edit().putBoolean(key, enabled).putLong(VERSION, version() + 1).apply();
        }
    }
    public boolean allows(String key) { return enabled() && selected(key); }
    public long version() { return preferences.getLong(VERSION, 0); }
}
