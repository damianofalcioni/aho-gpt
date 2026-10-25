package aho.gpt.android.ui;

import android.app.Activity;
import android.os.Build;
import android.speech.SpeechRecognizer;
import android.speech.tts.Voice;
import aho.gpt.R;
import aho.gpt.android.speech.SpeechAvailability;
import aho.gpt.android.speech.SpeechPreferences;
import java.util.List;
import java.util.Locale;

/** Reports provider inventory; explicit previews remain in the speech settings dialogs. */
final class SpeechDiagnostics implements AutoCloseable {
    private final Activity activity;
    private final DiagnosticRow input;
    private final DiagnosticRow output;
    private List<Voice> voices;
    private boolean loaded;

    SpeechDiagnostics(Activity activity) {
        this.activity = activity;
        input = new DiagnosticRow(activity, R.id.diagnostic_services, R.string.diagnostic_stt,
                () -> activity.findViewById(R.id.stt_settings).performClick());
        output = new DiagnosticRow(activity, R.id.diagnostic_services, R.string.diagnostic_tts,
                () -> activity.findViewById(R.id.tts_settings).performClick());
    }
    void checking() { loaded = false; voices = null; }
    void loaded(List<Voice> result) { voices = result; loaded = true; refresh(); }
    void refresh() {
        boolean available = SpeechAvailability.input(activity);
        boolean local = Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(activity);
        int inputStatus = available ? enabledStatus(SpeechPreferences.INPUT) : R.string.diagnostic_unavailable;
        input.update(inputStatus, statusColor(inputStatus),
                activity.getString(R.string.diagnostic_stt_help, choice(SpeechPreferences.INPUT),
                        SpeechPreferences.selectionLabel(activity, SpeechPreferences.INPUT),
                        activity.getString(local ? R.string.diagnostic_on_device : R.string.diagnostic_system_provider),
                        activity.getString(available ? R.string.diagnostic_available : R.string.diagnostic_unavailable)));
        int issue = playback();
        int outputStatus = issue == R.string.diagnostic_available ? enabledStatus(SpeechPreferences.OUTPUT) : issue;
        output.update(outputStatus, statusColor(outputStatus), activity.getString(R.string.diagnostic_tts_help,
                choice(SpeechPreferences.OUTPUT), SpeechPreferences.selectionLabel(activity, SpeechPreferences.OUTPUT),
                voiceLabel(), activity.getString(issue)));
    }
    private String choice(String key) {
        return activity.getString(SpeechPreferences.enabled(activity, key) ? R.string.diagnostic_on : R.string.diagnostic_off);
    }
    private String voiceLabel() {
        String selected = SpeechPreferences.voice(activity);
        return selected.isEmpty() ? activity.getString(R.string.default_voice) : selected;
    }
    private int playback() {
        if (!SpeechAvailability.googleAppEnabled(activity)) { return R.string.diagnostic_disabled; }
        if (!loaded) { return R.string.diagnostic_checking; }
        if (voices == null || voices.isEmpty()) { return R.string.diagnostic_unavailable; }
        String selected = SpeechPreferences.voice(activity);
        if (!selected.isEmpty()) { return selectedVoice(selected); }
        return language();
    }
    private int language() {
        Locale locale = SpeechPreferences.locale(activity, SpeechPreferences.OUTPUT);
        for (Voice voice : voices) {
            if (locale.getLanguage().equals(voice.getLocale().getLanguage())) { return R.string.diagnostic_available; }
        }
        return R.string.diagnostic_language_missing;
    }
    private int selectedVoice(String selected) {
        for (Voice voice : voices) {
            if (selected.equals(voice.getName())) { return R.string.diagnostic_available; }
        }
        return R.string.diagnostic_voice_missing;
    }
    private int enabledStatus(String key) {
        return SpeechPreferences.enabled(activity, key) ? R.string.diagnostic_available : R.string.diagnostic_off;
    }
    private int statusColor(int status) {
        if (status == R.string.diagnostic_available) { return R.color.diagnostic_ok; }
        if (status == R.string.diagnostic_off) { return R.color.diagnostic_warning; }
        return status == R.string.diagnostic_checking ? R.color.diagnostic_warning : R.color.diagnostic_error;
    }
    @Override public void close() { input.close(); output.close(); }
}
