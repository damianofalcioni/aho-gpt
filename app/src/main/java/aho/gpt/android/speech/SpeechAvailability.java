package aho.gpt.android.speech;

import android.Manifest;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import aho.gpt.R;

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
    public static int inputIssue(Context context, boolean available) {
        if (!SpeechPreferences.enabled(context, SpeechPreferences.INPUT)) { return R.string.stt_disabled; }
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return R.string.microphone_permission_required;
        }
        if (!googleAppEnabled(context)) { return R.string.speech_provider_disabled; }
        return available ? 0 : R.string.speech_unavailable;
    }
}
