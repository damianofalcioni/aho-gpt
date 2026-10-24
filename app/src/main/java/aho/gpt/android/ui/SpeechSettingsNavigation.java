package aho.gpt.android.ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.provider.Settings;
import android.widget.Toast;
import aho.gpt.R;

final class SpeechSettingsNavigation {
    private SpeechSettingsNavigation() { }

    static void open(Activity activity, boolean playback) {
        String action = playback ? "com.android.settings.TTS_SETTINGS" : Settings.ACTION_VOICE_INPUT_SETTINGS;
        try { activity.startActivity(new Intent(action)); }
        catch (ActivityNotFoundException | SecurityException exception) { fallback(activity); }
    }
    private static void fallback(Activity activity) {
        try { activity.startActivity(new Intent(Settings.ACTION_SETTINGS)); }
        catch (ActivityNotFoundException | SecurityException exception) {
            Toast.makeText(activity, R.string.setting_unavailable, Toast.LENGTH_SHORT).show();
        }
    }
}
