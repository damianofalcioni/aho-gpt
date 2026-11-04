package aho.gpt.android.access;

import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;

/** Reads the user's grant independently of the accessibility service's current connection. */
public final class ScreenAccessGrant {
    private ScreenAccessGrant() { }

    public static boolean granted(Context context) {
        if (Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED, 0) != 1) {
            return false;
        }
        String enabled = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return contains(enabled, new ComponentName(context, AssistantAccessibilityService.class));
    }
    static boolean contains(String enabled, ComponentName service) {
        if (enabled == null) { return false; }
        for (String entry : enabled.split(":")) {
            if (service.equals(ComponentName.unflattenFromString(entry))) { return true; }
        }
        return false;
    }
}
