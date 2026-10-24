package aho.gpt.android.speech;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

/** An absent Google app does not prevent use of another installed speech provider. */
public final class SpeechAvailability {
    private SpeechAvailability() { }

    public static boolean googleAppEnabled(Context context) {
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo("com.google.android.googlequicksearchbox", 0);
            return info.enabled;
        } catch (PackageManager.NameNotFoundException exception) { return true; }
    }
    public static boolean input(Context context) {
        return googleAppEnabled(context) && SpeechInput.available(context);
    }
}
