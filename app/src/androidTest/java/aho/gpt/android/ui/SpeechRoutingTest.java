package aho.gpt.android.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.Switch;
import aho.gpt.R;
import aho.gpt.android.net.ProviderSettings;
import aho.gpt.android.speech.SpeechAvailability;
import aho.gpt.android.speech.SpeechInput;
import aho.gpt.android.speech.SpeechPreferences;
import aho.gpt.android.speech.VoiceSpeech;
import aho.gpt.custom.AudioMode;
import java.util.ArrayList;
import java.util.Map;

/** Checks server routing without querying Android providers, recording or contacting a server. */
final class SpeechRoutingTest implements SpeechInput.Listener {
    private static final String CUSTOM = "custom_enabled";
    private static final String SPEECH = "custom_speech_apis";
    private static final String REALTIME = "custom_realtime_api";
    private static final String LIVE = "custom_live_api";
    private final Activity activity;
    SpeechRoutingTest(Activity activity) { this.activity = activity; }

    void run() {
        SharedPreferences preferences = activity.getSharedPreferences("settings", Context.MODE_PRIVATE);
        Map<String, ?> saved = preferences.getAll();
        try (SpeechSettingsRows rows = new SpeechSettingsRows(activity)) {
            for (boolean enabled : new boolean[]{false, true}) {
                SpeechPreferences.enable(activity, SpeechPreferences.INPUT, enabled);
                SpeechPreferences.enable(activity, SpeechPreferences.OUTPUT, enabled);
                for (AudioMode mode : new AudioMode[]{AudioMode.SPEECH, AudioMode.REALTIME, AudioMode.LIVE}) {
                    select(preferences, mode); rows.refresh();
                    switches(enabled); routing(); listening(mode);
                }
                preferences.edit().putBoolean(CUSTOM, false).apply();
                require(new ProviderSettings(activity).voiceAudioMode() == AudioMode.PLATFORM, "Disabled Custom must use platform speech");
                require(SpeechPreferences.voiceEnabled(activity, SpeechPreferences.INPUT) == enabled, "Restore the platform STT choice");
                require(SpeechPreferences.voiceEnabled(activity, SpeechPreferences.OUTPUT) == enabled, "Restore the platform TTS choice");
            }
        } finally { restore(preferences, saved); }
    }
    private void select(SharedPreferences preferences, AudioMode mode) {
        preferences.edit().putBoolean(CUSTOM, true).putBoolean(SPEECH, mode == AudioMode.SPEECH)
                .putBoolean(REALTIME, mode == AudioMode.REALTIME).putBoolean(LIVE, mode == AudioMode.LIVE).apply();
    }
    private void switches(boolean saved) {
        for (int id : new int[]{R.id.stt_enabled, R.id.tts_enabled}) {
            Switch control = activity.findViewById(id);
            require(control.isEnabled() && !control.isChecked() && control.getAlpha() == 0.5f,
                    "Unused switches must stay muted and accept an explanation tap");
            control.performClick();
            require(!control.isChecked(), "An explanation tap must leave the switch off");
        }
        require(SpeechPreferences.enabled(activity, SpeechPreferences.INPUT) == saved, "Keep the saved STT choice");
        require(SpeechPreferences.enabled(activity, SpeechPreferences.OUTPUT) == saved, "Keep the saved TTS choice");
        require(SpeechPreferences.voiceEnabled(activity, SpeechPreferences.INPUT), "Server input must ignore the platform switch");
        require(SpeechPreferences.voiceEnabled(activity, SpeechPreferences.OUTPUT), "Server playback must ignore the platform switch");
        require(activity.findViewById(R.id.tts_provider_status).getVisibility() == View.GONE, "Hide unused provider status");
        require(activity.findViewById(R.id.stt_settings).isEnabled(), "Keep the language gear and local tests available");
    }
    private void routing() {
        Context context = new ContextWrapper(activity) {
            @Override public PackageManager getPackageManager() { throw new AssertionError("Must not query Android speech providers"); }
            @Override public int checkSelfPermission(String permission) { return PackageManager.PERMISSION_GRANTED; }
        };
        require(SpeechAvailability.voiceInput(context), "Server input needs no Android provider inventory");
        require(SpeechAvailability.inputIssue(context, true) == 0, "Server input must work with platform STT off");
        try (VoiceSpeech speech = new VoiceSpeech(context, this)) {
            require(speech.available(), "Server speech must initialize without Android providers");
        }
        Context denied = new ContextWrapper(context) {
            @Override public int checkSelfPermission(String permission) {
                return Manifest.permission.RECORD_AUDIO.equals(permission) ? PackageManager.PERMISSION_DENIED : super.checkSelfPermission(permission);
            }
        };
        require(SpeechAvailability.inputIssue(denied, true) == R.string.microphone_permission_required, "Server speech still needs microphone permission");
    }
    private void listening(AudioMode mode) {
        VoicePanel panel = new VoicePanel(activity);
        VoicePresentation presentation = new VoicePresentation(activity, panel);
        String language = SpeechPreferences.label(activity, SpeechPreferences.locale(activity, SpeechPreferences.INPUT));
        for (boolean followUp : new boolean[]{false, true}) {
            presentation.listening(followUp);
            ArrayList<View> matches = new ArrayList<>();
            panel.findViewsWithText(matches, language, View.FIND_VIEWS_WITH_TEXT);
            require(!matches.isEmpty() == mode.usesRecognitionLanguage(), "Show a listening language only when used");
        }
        presentation.listeningStopped(); presentation.closeOverlay();
    }
    private void restore(SharedPreferences preferences, Map<String, ?> saved) {
        SharedPreferences.Editor editor = preferences.edit();
        for (String key : new String[]{CUSTOM, SPEECH, REALTIME, LIVE, SpeechPreferences.INPUT + "_enabled", SpeechPreferences.OUTPUT + "_enabled"}) {
            if (saved.containsKey(key)) { editor.putBoolean(key, (Boolean) saved.get(key)); }
            else { editor.remove(key); }
        }
        require(editor.commit(), "Could not restore speech preferences");
    }
    @Override public void began() { throw new AssertionError("Must not record audio"); }
    @Override public void result(String text) { throw new AssertionError("Must not recognize speech"); }
    @Override public void error(int error) { throw new AssertionError("Must not start speech"); }
    private void require(boolean condition, String message) { if (!condition) { throw new AssertionError(message); } }
}
