package aho.gpt.android.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import aho.gpt.R;
import aho.gpt.android.speech.PlaybackSettingsClient;
import aho.gpt.android.speech.SpeechAvailability;
import aho.gpt.android.speech.SpeechPreferences;

/** Keeps saved user choices separate from current provider availability. */
final class SpeechSettingsRows implements AutoCloseable {
    private final Activity activity;
    private final PlaybackSettingsClient probe;
    private final PlaybackSettingsDialog playback;
    private final RecognitionSettingsDialog recognition;
    private AlertDialog information;

    SpeechSettingsRows(Activity activity) {
        this.activity = activity;
        probe = new PlaybackSettingsClient(activity);
        playback = new PlaybackSettingsDialog(activity); recognition = new RecognitionSettingsDialog(activity);
        activity.findViewById(R.id.tts_settings).setOnClickListener(view -> playback.show());
        activity.findViewById(R.id.stt_settings).setOnClickListener(view -> recognition.show());
        activity.findViewById(R.id.tts_info).setOnClickListener(view -> info(R.string.tts_language, R.string.tts_info, R.id.tts_provider_status));
        activity.findViewById(R.id.stt_info).setOnClickListener(view -> info(R.string.stt_language, R.string.stt_info, R.id.stt_provider_status));
    }
    void refresh() {
        boolean google = SpeechAvailability.googleAppEnabled(activity);
        boolean input = SpeechAvailability.input(activity);
        toggle(R.id.stt_enabled, SpeechPreferences.INPUT, input);
        toggle(R.id.tts_enabled, SpeechPreferences.OUTPUT, false);
        providerStatus(R.id.stt_provider_status, input ? R.string.stt_ready
                : google ? R.string.speech_unavailable : R.string.speech_provider_disabled, input);
        providerStatus(R.id.tts_provider_status, R.string.language_support_loading, false);
        probe.load(voices -> {
            boolean available = google && voices != null && !voices.isEmpty();
            toggle(R.id.tts_enabled, SpeechPreferences.OUTPUT, available);
            providerStatus(R.id.tts_provider_status, available ? R.string.tts_ready
                    : google ? R.string.tts_provider_unavailable : R.string.speech_provider_disabled, available);
            probe.close();
        });
    }
    private void providerStatus(int id, int message, boolean available) {
        TextView label = activity.findViewById(id);
        label.setText(message); label.setVisibility(available ? View.GONE : View.VISIBLE);
    }
    private void toggle(int id, String key, boolean available) {
        Switch control = activity.findViewById(id);
        control.setOnCheckedChangeListener(null);
        control.setEnabled(available); control.setChecked(available && SpeechPreferences.enabled(activity, key));
        control.setOnCheckedChangeListener((button, checked) -> SpeechPreferences.enable(activity, key, checked));
    }
    private void info(int title, int message, int statusId) {
        TextView status = activity.findViewById(statusId);
        information = new AlertDialog.Builder(activity).setTitle(title)
                .setMessage(activity.getString(R.string.setting_info_with_status, status.getText(), activity.getString(message)))
                .setPositiveButton(R.string.close, null).show();
    }
    void permissionResult(boolean granted) { if (granted) { recognition.test(); } }
    void stopTests() { recognition.stopTest(); playback.close(); }
    @Override public void close() {
        probe.close(); playback.close(); recognition.close();
        if (information != null) { information.dismiss(); information = null; }
    }
}
