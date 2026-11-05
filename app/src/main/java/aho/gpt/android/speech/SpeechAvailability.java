package aho.gpt.android.speech;

import android.Manifest;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import aho.gpt.R;
import aho.gpt.android.net.ProviderSettings;

/** Speech availability depends on providers, never on the Google app alone. */
public final class SpeechAvailability {
    private SpeechAvailability() { }

    public static boolean googleAppDisabled(Context context) {
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo("com.google.android.googlequicksearchbox", 0);
            return !info.enabled;
        } catch (PackageManager.NameNotFoundException exception) { return false; }
    }
    public static boolean input(Context context) {
        return SpeechInput.available(context);
    }
    public static boolean voiceInput(Context context) { return new ProviderSettings(context).serverAudio() || input(context); }
    public static int inputIssue(Context context, boolean available) {
        if (!SpeechPreferences.voiceEnabled(context, SpeechPreferences.INPUT)) { return R.string.stt_disabled; }
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return R.string.microphone_permission_required;
        }
        return available ? 0 : R.string.speech_unavailable;
    }
}
